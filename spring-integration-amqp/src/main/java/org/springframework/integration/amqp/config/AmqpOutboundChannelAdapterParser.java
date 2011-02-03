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

package org.springframework.integration.amqp.config;

import org.w3c.dom.Element;

import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.config.xml.AbstractOutboundChannelAdapterParser;
import org.springframework.integration.config.xml.IntegrationNamespaceUtils;
import org.springframework.util.StringUtils;

/**
 * Parser for the AMQP 'outbound-channel-adapter' element.
 * 
 * @author Mark Fisher
 * @since 2.1
 */
public class AmqpOutboundChannelAdapterParser extends AbstractOutboundChannelAdapterParser {

	@Override
	protected AbstractBeanDefinition parseConsumer(Element element, ParserContext parserContext) {
		BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(
				"org.springframework.integration.amqp.AmqpOutboundEndpoint");
		String amqpTemplateRef = element.getAttribute("amqp-template");
		if (!StringUtils.hasText(amqpTemplateRef)) {
			amqpTemplateRef = "amqpTemplate";
		}
		builder.addConstructorArgReference(amqpTemplateRef);
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "exchange-name");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "routing-key");
		return builder.getBeanDefinition();
	}

}
