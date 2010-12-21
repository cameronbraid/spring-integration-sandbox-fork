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

package org.acme.echo.module.config;

import org.w3c.dom.Element;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.config.xml.AbstractConsumerEndpointParser;
import org.springframework.integration.config.xml.IntegrationNamespaceUtils;

/**
 * Handles parsing module outbound-gateway
 *
 * @author Oleg Zhurakousky
 */
public class ModuleOutboundGatewayParser extends AbstractConsumerEndpointParser {

	@Override
	protected String getInputChannelAttributeName() {
		return "request-channel";
	}

	@Override
	protected BeanDefinitionBuilder parseHandler(Element element, ParserContext parserContext) {
		
		BeanDefinitionBuilder moduleContextBuilder = BeanDefinitionBuilder.genericBeanDefinition(
				"org.springframework.context.support.ClassPathXmlApplicationContext");
		
		String namespaceUri = element.getNamespaceURI();
		String nsPrefix = namespaceUri.substring(namespaceUri.lastIndexOf('/')+1);
		moduleContextBuilder.addConstructorArgValue("META-INF/spring/module/" + nsPrefix + "-module-context.xml");
		
		BeanDefinitionBuilder exchangerBuilder = BeanDefinitionBuilder.genericBeanDefinition(
				"org.acme.echo.module.ModuleGatewayRequestExchanger");	
		exchangerBuilder.addConstructorArgValue(moduleContextBuilder.getBeanDefinition());
		
		BeanDefinitionBuilder moduleHandlerBuilder = BeanDefinitionBuilder.genericBeanDefinition(
				"org.acme.echo.module.ModuleGatewayMessageHandler");
		moduleHandlerBuilder.addConstructorArgValue(exchangerBuilder.getBeanDefinition());
		// TODO do we need to support error-channel at the module or should it be outside (I think we do)
		//IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "error-handler");
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(moduleHandlerBuilder, element, "reply-channel", "outputChannel");
		
		return moduleHandlerBuilder;
	}

}
