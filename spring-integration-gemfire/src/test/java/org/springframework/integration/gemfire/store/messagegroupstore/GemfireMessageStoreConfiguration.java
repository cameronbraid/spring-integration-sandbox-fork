package org.springframework.integration.gemfire.store.messagegroupstore;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.util.Arrays;

/**
 * configures the machinery for our aggregator
 *
 * @author Josh Long
 */
@Configuration
public class GemfireMessageStoreConfiguration {

	public static void main(String [] args) throws Throwable {
		ClassPathXmlApplicationContext classPathXmlApplicationContext
		 = new ClassPathXmlApplicationContext(
				"org/springframework/integration" +
				"/gemfire/store/messagegroupstore/" +
				"GemfireMessageStore-context.xml");

		Producer producer = classPathXmlApplicationContext.getBean( Producer.class );
		producer.sendManyMessages( Arrays.asList("1,2,3,4,5".split(",")));

	}

}
