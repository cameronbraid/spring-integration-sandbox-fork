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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Test the baseline Checkpoint configuration
 * 
 * @author David Turanski
 *
 */

public class CheckpointedLoanBrokerTest extends AbstractCheckpointedLoanBrokerTest {
  
	public void setUp(){

	}
	
	@Override
	protected String[] getContextConfigLocations() {	
		return new String[]{"CheckpointedLoanBrokerTest-context.xml"};
	}
	
	@Test
	public void testTotalCheckpointCount() {
		long waitTime = waitForProcessToEnd();
		logger.info("waited " + waitTime + " ms for process to end.");
	    logger.info("checkpoint count = " + messageCounter.getCount());
		assertTrue(messageCounter.getCount() > 0);
		assertEquals(messageCounter.getCount(),checkpointDetector.getCount());
		assertTrue("Process did not complete in time", checkpointDetector.isProcessEnded());
	}
	
	@Test
	public void testCloseContext() {
		logger.info("closing context...");
		context.close();
		logger.info("closed context");
		
	    long waitTime = waitForProcessToEnd();
		logger.info("waited " + waitTime + " ms for process to end.");
		assertTrue(messageCounter.getCount() > 0);
		assertEquals(messageCounter.getCount(),checkpointDetector.getCount());
		assertTrue("Process did not complete in time", checkpointDetector.isProcessEnded());
	}
	 

	private long waitForProcessToEnd(){
		long waitTime = 0;
		while (!checkpointDetector.isProcessEnded() && waitTime <= 3000) {
		  try {	
			 Thread.sleep(100);
	         waitTime += 100;
		  } catch (InterruptedException e) {
			// TODO Auto-generated catch block
			 e.printStackTrace();
		  }
		}
		return waitTime;
	}

	
}
