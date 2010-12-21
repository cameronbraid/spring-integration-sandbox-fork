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
package org.acme.echo.module.config;

import static junit.framework.Assert.assertEquals;

import org.junit.Test;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.Message;
import org.springframework.integration.gateway.RequestReplyExchanger;
import org.springframework.integration.message.GenericMessage;

/**
 * @author Oleg Zhurakousky
 *
 */
public class ModuleConfigTest {

	@Test
	public void testModule(){
		ApplicationContext ac = new ClassPathXmlApplicationContext("integration-config.xml", this.getClass());
		RequestReplyExchanger gateway = ac.getBean("echoGateway", RequestReplyExchanger.class);
		Message<?> replyMessage = gateway.exchange(new GenericMessage<String>("foo"));
		assertEquals("FOO", replyMessage.getPayload());
	}
}
