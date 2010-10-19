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

package org.springframework.integration.amqp;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageListener;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.amqp.support.converter.SimpleMessageConverter;
import org.springframework.integration.endpoint.MessageProducerSupport;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.util.Assert;

/**
 * Adapter that receives Messages from an AMQP Queue, converts them into
 * Spring Integration Messages, and sends the results to a Message Channel.
 * 
 * @author Mark Fisher
 * @since 2.0
 */
public class AmqpInboundEndpoint extends MessageProducerSupport {

	private final SimpleMessageListenerContainer messageListenerContainer;

	private final MessageConverter converter = new SimpleMessageConverter();


	public AmqpInboundEndpoint(ConnectionFactory connectionFactory) {
		Assert.notNull(connectionFactory, "ConnectionFactory must not be null");
		this.messageListenerContainer = new SimpleMessageListenerContainer(connectionFactory);
		this.messageListenerContainer.setAutoStartup(false);
	}


	public void setQueueName(String queueName) {
		this.messageListenerContainer.setQueueName(queueName);
	}

	@Override
	protected void onInit() {
		this.messageListenerContainer.setMessageListener(new MessageListener() {
			@Override
			public void onMessage(Message message) {
				Object payload = converter.fromMessage(message);
				sendMessage(MessageBuilder.withPayload(payload)
						.copyHeaders(message.getMessageProperties().getHeaders()).build());
			}
		});
		this.messageListenerContainer.afterPropertiesSet();
		super.onInit();
	}

	@Override
	protected void doStart() {
		this.messageListenerContainer.start();
	}

	@Override
	protected void doStop() {
		this.messageListenerContainer.stop();
	}

}
