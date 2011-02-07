/*
 * Copyright 2002-2011 the original author or authors.
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

import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.integration.Message;
import org.springframework.integration.handler.AbstractReplyProducingMessageHandler;
import org.springframework.util.Assert;

/**
 * Adapter that converts and sends Messages to an AMQP Exchange.
 * 
 * @author Mark Fisher
 * @since 2.1
 */
public class AmqpOutboundEndpoint extends AbstractReplyProducingMessageHandler {

	private final AmqpTemplate amqpTemplate;

	private volatile String exchangeName = "";

	private volatile String routingKey = "";

	private volatile boolean expectReply;


	public AmqpOutboundEndpoint(AmqpTemplate amqpTemplate) {
		Assert.notNull(amqpTemplate, "AmqpTemplate must not be null");
		this.amqpTemplate = amqpTemplate;
	}


	public void setExchangeName(String exchangeName) {
		this.exchangeName = exchangeName;
	}

	public void setRoutingKey(String routingKey) {
		this.routingKey = routingKey;
	}

	public void setExpectReply(boolean expectReply) {
		this.expectReply = expectReply;
	}

	@Override
	protected Object handleRequestMessage(Message<?> requestMessage) {
		if (this.expectReply) {
			return this.sendAndReceive(requestMessage);
		}
		else {
			this.send(requestMessage);
			return null;
		}
	}

	private void send(Message<?> requestMessage) {
		this.amqpTemplate.convertAndSend(this.exchangeName, this.routingKey, requestMessage.getPayload(), new MessagePostProcessor() {
			@Override
			public org.springframework.amqp.core.Message postProcessMessage(org.springframework.amqp.core.Message message) throws AmqpException {
				//message.getMessageProperties().setX
				return message;
			}
		});
	}

	private Object sendAndReceive(Message<?> requestMessage) {
		// TODO: remove cast once the interface methods are added
		return ((RabbitTemplate) this.amqpTemplate).convertSendAndReceive(this.exchangeName, this.routingKey, requestMessage.getPayload());
	}

}
