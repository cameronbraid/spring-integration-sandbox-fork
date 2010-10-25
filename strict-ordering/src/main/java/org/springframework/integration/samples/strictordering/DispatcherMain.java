package org.springframework.integration.samples.strictordering;

import org.springframework.context.support.ClassPathXmlApplicationContext;

public class DispatcherMain {
	 
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		 new ClassPathXmlApplicationContext(
				"META-INF/spring/jms-infrastructure-config.xml",
				"META-INF/spring/dispatcher-integration-config.xml",
				"META-INF/spring/cache-config.xml"
				);
		 
	}

}
