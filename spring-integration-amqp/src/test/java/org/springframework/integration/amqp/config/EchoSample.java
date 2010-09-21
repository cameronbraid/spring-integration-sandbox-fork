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

package org.springframework.integration.amqp.config;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.amqp.rabbit.connection.SingleConnectionFactory;
import org.springframework.amqp.rabbit.core.ChannelCallback;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.rabbitmq.client.Channel;

/**
 * @author Mark Fisher
 * @since 2.0
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class EchoSample {

	@BeforeClass
	public static void setup() {
		SingleConnectionFactory connectionFactory = new SingleConnectionFactory();
		connectionFactory.setUsername("guest");
		connectionFactory.setPassword("guest");
		RabbitTemplate template = new RabbitTemplate(connectionFactory);
		template.execute(new ChannelCallback<Object>() {
			@Override
			public Object doInRabbit(Channel channel) throws Exception {
				channel.queueDeclare("si.test.queue", true, false, false, null);
				channel.exchangeDeclare("si.test.exchange", "direct", true);
				channel.queueBind("si.test.queue", "si.test.exchange", "si.test.binding");
				return null;
			}
		});
	}

	@Test
	public void run() throws Exception {
		Thread.sleep(60 * 1000);
	}

}
