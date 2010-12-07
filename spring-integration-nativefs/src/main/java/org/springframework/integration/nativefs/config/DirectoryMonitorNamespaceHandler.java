package org.springframework.integration.nativefs.config;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.AbstractSingleBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.config.xml.AbstractIntegrationNamespaceHandler;
import org.springframework.integration.config.xml.IntegrationNamespaceUtils;
import org.springframework.integration.nativefs.DirectoryMonitorInboundFileEndpoint;
import org.w3c.dom.Element;

/**
 * Namespace parser that encapsulates the logic for the {@link org.springframework.integration.nativefs.DirectoryMonitorInboundFileEndpoint}
 *
 * @author Josh Long
 * @since 1.0
 */
public class DirectoryMonitorNamespaceHandler extends AbstractIntegrationNamespaceHandler {

	public void init() {
		registerBeanDefinitionParser("inbound-channel-adapter", new DirectoryMonitorInboundFileEndpointParser());
	}


	/**
	 * Responsible for parsing the XML for this adapter.
	 * <p/>
	 * Since the inbound adapter has no Spring Integration-specific nuances, we simply use the core Spring framework options.
	 *
	 * @author Josh Long
	 */
	public class DirectoryMonitorInboundFileEndpointParser extends AbstractSingleBeanDefinitionParser {

		@Override
		protected String getBeanClassName(Element element) {
			return DirectoryMonitorInboundFileEndpoint.class.getName();
		}

		@Override
		protected boolean shouldGenerateIdAsFallback() {
			return true;
		}

		@Override
		protected boolean shouldGenerateId() {
			return false;
		}

		@Override
		protected void doParse(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
			IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "directory", "directoryToMonitor");
			IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "channel", "outputChannel");
		}
	}

}

