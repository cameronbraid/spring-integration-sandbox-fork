/*
 * Copyright 2002-2010 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.checkpoint;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.log4j.Logger;
import org.junit.Before;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.Message;
import org.springframework.integration.MessageChannel;
import org.springframework.integration.channel.AbstractMessageChannel;
import org.springframework.integration.channel.interceptor.ChannelInterceptorAdapter;
import org.springframework.integration.checkpoint.Checkpoint;
import org.springframework.integration.samples.loanbroker.LoanBrokerGateway;
import org.springframework.integration.samples.loanbroker.domain.Customer;
import org.springframework.integration.samples.loanbroker.domain.LoanRequest;


/**
 * A base class for integration testing Checkpoint configurations with the Loan Broker example.
 * 
 * 
 * @author David Turanski
 * 
 */

public abstract class AbstractCheckpointedLoanBrokerTest {

	protected Logger logger = Logger.getLogger(this.getClass());
	protected ConfigurableApplicationContext context;
	 
	protected CheckpointDetector checkpointDetector;
	protected MessageCounter messageCounter;
	
	/**
	 * Implement to set the context configuration file(s)
	 * @return
	 */
	protected abstract String[] getContextConfigLocations();
	
	/**
	 * Implement to perform additional set up. Do not annotate this with @Before since this 
	 * class will invoke within its own set up.
	 */
	protected abstract void setUp();
	
	/**
	 *   Wire up the monitor and checkpoint channel interceptors and run the loan broker request
	 */
	@Before
	public void doSetUp(){
		context = new ClassPathXmlApplicationContext(getContextConfigLocations());
		AbstractMessageChannel monitorChannel = context.getBean("monitorChannel", AbstractMessageChannel.class);
		AbstractMessageChannel checkpointChannel = context.getBean("checkpointChannel", AbstractMessageChannel.class);
	    
		checkpointDetector = new CheckpointDetector();
		messageCounter = new MessageCounter();
		
		checkpointChannel.addInterceptor(messageCounter);
		monitorChannel.addInterceptor(checkpointDetector);
		setUp();
		// Run the quote request
		runLoanBrokerRequest();
	}
		 
	protected void runLoanBrokerRequest(){
		LoanBrokerGateway broker = context.getBean("loanBrokerGateway", LoanBrokerGateway.class);

		LoanRequest loanRequest = new LoanRequest();
		loanRequest.setCustomer(new Customer());
		broker.getBestLoanQuote(loanRequest);
	}

 
	/**
	 * A {@link ChannelInterceptor} that counts the number of messages on a channel. Used to 
	 * count the Checkpoint messages for testing
	 * 
	 * @author David Turanski
	 *
	 */
	protected class MessageCounter extends ChannelInterceptorAdapter {
		protected AtomicInteger count = new AtomicInteger();

		public int getCount() {
			return count.intValue();
		}
		
		public Message<?> preSend(Message<?> message, MessageChannel messageChannel) {
			count.incrementAndGet(); 
			return message;
		}
		 
	}
    
	/**
	 * A {@link ChannelInterceptor} to track Checkpoints contained in the aggregated messages.
	 * Counts the number of Checkpoints, detects when the Loan Broker process has ended, and can
	 * provide the list of Checkpoints on the monitor channel 
	 * 
	 * @author David Turanski
	 *
	 */
	protected class CheckpointDetector extends MessageCounter {
		private boolean processEnded;
		private List<Checkpoint> checkpoints = new ArrayList<Checkpoint>();
		
		public Message<?> preSend(Message<?> message, MessageChannel messageChannel) {
			@SuppressWarnings("unchecked")
			List<Checkpoint> list = (List<Checkpoint>) message.getPayload();
			logger.info("checkpointDetector received " + list.size() + " checkpoints");
			count.addAndGet(list.size());
			processEnded = processEnded || checkProcessEnded(list);
			checkpoints.addAll(list);
			return message;
		}
		
		private boolean checkProcessEnded(List<Checkpoint> list) {
			for (Checkpoint checkpoint: list){
				if (checkpoint.getChannelName().equals("loanBrokerReplyChannel") ){
					return true;
				}
			}
			return false;
			
		}
		/**
		 * True if a Checkpoint was received on the monitor Channel corresponding to the loanBrokerReplyChannel
		 * If this is true, then the last expected Checkpoint was sent to the monitor Channel indicating a normal
		 * end state
		 * 
		 * @return
		 */
		public boolean isProcessEnded(){
			return processEnded;
		}
		
		/**
		 * 
		 * @return the checkpoints
		 */
		public List<Checkpoint> getCheckpoints(){
			return checkpoints;
		}
	}
}
