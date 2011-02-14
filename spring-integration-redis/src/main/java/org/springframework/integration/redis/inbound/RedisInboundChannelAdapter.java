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

package org.springframework.integration.redis.inbound;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.keyvalue.redis.connection.RedisConnectionFactory;
import org.springframework.data.keyvalue.redis.listener.ChannelTopic;
import org.springframework.data.keyvalue.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.keyvalue.redis.listener.adapter.MessageListenerAdapter;
import org.springframework.data.keyvalue.redis.serializer.StringRedisSerializer;
import org.springframework.integration.endpoint.MessageProducerSupport;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.util.Assert;

/**
 * @author Mark Fisher
 * @since 2.1
 */
public class RedisInboundChannelAdapter extends MessageProducerSupport {

	private final RedisMessageListenerContainer container = new RedisMessageListenerContainer();

	private volatile String[] topics;


	public RedisInboundChannelAdapter(RedisConnectionFactory connectionFactory) {
		Assert.notNull(connectionFactory, "connectionFactory must not be null");
		this.container.setConnectionFactory(connectionFactory);
	}


	public void setTopics(String... topics) {
		this.topics = topics;
	}

	@Override
	protected void onInit() {
		super.onInit();
		Assert.notEmpty(this.topics, "at least one topis is required for subscription");
		MessageListenerDelegate delegate = new MessageListenerDelegate();
		MessageListenerAdapter adapter = new MessageListenerAdapter(delegate);
		adapter.setSerializer(new StringRedisSerializer());
		List<ChannelTopic> topicList = new ArrayList<ChannelTopic>();
		for (String topic : this.topics) {
			topicList.add(new ChannelTopic(topic));
		}
		this.container.afterPropertiesSet();
		this.container.addMessageListener(adapter, topicList);
	}


	private class MessageListenerDelegate {

		@SuppressWarnings("unused")
		public void handleMessage(String s) {
			sendMessage(MessageBuilder.withPayload(s).build());
		}
	}

}
