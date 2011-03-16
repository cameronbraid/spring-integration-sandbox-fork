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

import com.rabbitmq.client.impl.Frame;

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

	public class HeaderCopyMessagePostProcessor implements MessagePostProcessor {
		final private Message<?> requestMessage;
		public HeaderCopyMessagePostProcessor(Message<?> requestMessage) {
			this.requestMessage = requestMessage;
		}
		@Override
		public org.springframework.amqp.core.Message postProcessMessage(org.springframework.amqp.core.Message message) throws AmqpException {
			for (String headerName : requestMessage.getHeaders().keySet()) {
					Object headerValue = requestMessage.getHeaders().get(headerName);
					boolean isHeaderSupported = includeMessageHeader(headerName, headerValue);
					if (isHeaderSupported) {
						message.getMessageProperties().setHeader(headerName, headerValue);
					}
					
			}
			return message;
		}
	}

	protected boolean includeMessageHeader(String headerName, Object headerValue) {
		boolean isHeaderSupported = false;
		try {
			Frame.fieldValueSize(headerValue);
			isHeaderSupported = true;
		}
		catch (Exception e) {
			// Frame.fieldValueSize throws an exception for invalid header value types
		}
		return isHeaderSupported;
	}

	private void send(final Message<?> requestMessage) {
		this.amqpTemplate.convertAndSend(this.exchangeName, this.routingKey, requestMessage.getPayload(), new HeaderCopyMessagePostProcessor(requestMessage));
	}

	private Object sendAndReceive(Message<?> requestMessage) {
		// TODO: remove cast once the interface methods are added
		// TODO: ask spring to add convertSendAndReceive method that can take a MessagePostProcessor (in this case a HeaderCopyMessagePostProcessor)
		return ((RabbitTemplate) this.amqpTemplate).convertSendAndReceive(this.exchangeName, this.routingKey, requestMessage.getPayload());
	}

}
