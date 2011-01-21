/*
 * Copyright 2007-2011 the original author or authors
 *
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 */

package org.springframework.integration.redis.outbound;

import org.springframework.data.keyvalue.redis.connection.RedisConnectionFactory;
import org.springframework.data.keyvalue.redis.core.StringRedisTemplate;
import org.springframework.integration.Message;
import org.springframework.integration.MessagingException;
import org.springframework.integration.core.MessageHandler;
import org.springframework.util.Assert;

/**
 * @author Mark Fisher
 * @since 2.1
 */
public class RedisPublishingMessageHandler implements MessageHandler {

	private final StringRedisTemplate template;

	private volatile String defaultRedisChannel;


	public RedisPublishingMessageHandler(RedisConnectionFactory connectionFactory) {
		Assert.notNull(connectionFactory, "connectionFactory must not be null");
		this.template = new StringRedisTemplate(connectionFactory);
		this.template.afterPropertiesSet();
	}


	public void setDefaultRedisChannel(String defaultRedisChannel) {
		this.defaultRedisChannel = defaultRedisChannel;
	}

	public void handleMessage(Message<?> message) throws MessagingException {
		String targetChannel = this.determineTargetChannel(message);
		this.template.convertAndSend(targetChannel, message.getPayload().toString());
	}

	private String determineTargetChannel(Message<?> message) {
		// TODO: add support for determining channel by evaluating SpEL against the Message
		Assert.hasText(this.defaultRedisChannel, "Failed to determine Redis target channel " +
				"from Message, and no defaultRedisChannel has been provided.");
		return this.defaultRedisChannel;
	}

}
