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
package org.acme.echo.module;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.integration.Message;
import org.springframework.integration.MessageChannel;
import org.springframework.integration.gateway.GatewayProxyFactoryBean;
import org.springframework.integration.gateway.RequestReplyExchanger;

/**
 * @author Oleg Zhurakousky
 *
 */
public class ModuleGatewayRequestExchanger implements RequestReplyExchanger, InitializingBean{
	
	private final ApplicationContext moduleContext;
	private volatile RequestReplyExchanger requestReplyExchanger;
	
	public ModuleGatewayRequestExchanger(ApplicationContext moduleContext){
		this.moduleContext = moduleContext;
	}

	@Override
	public void afterPropertiesSet() throws Exception {	
		GatewayProxyFactoryBean gatewayFactory = new GatewayProxyFactoryBean();
		String requestChannel = "echoStartChannel"; // we might want to get from some module.properties file
		String replyChannel = "echoEndChannel";
		gatewayFactory.setDefaultRequestChannel(moduleContext.getBean(requestChannel, MessageChannel.class));
		gatewayFactory.setDefaultReplyChannel(moduleContext.getBean(replyChannel, MessageChannel.class));
		gatewayFactory.afterPropertiesSet();
		requestReplyExchanger = (RequestReplyExchanger) gatewayFactory.getObject();
	}

	@Override
	public Message<?> exchange(Message<?> request) {
		return requestReplyExchanger.exchange(request);
	}

}
