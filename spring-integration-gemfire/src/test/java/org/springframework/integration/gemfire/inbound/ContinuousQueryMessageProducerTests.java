package org.springframework.integration.gemfire.inbound;

import org.springframework.context.support.ClassPathXmlApplicationContext;

/***
 * Tests the ability to react to the continuous query
 *
 * @author Josh Long
 */
public class ContinuousQueryMessageProducerTests {

	public static void main(String [] args) throws Exception {

		ClassPathXmlApplicationContext classPathXmlApplicationContext = new ClassPathXmlApplicationContext(
		  "org/springframework/integration/gemfire/inbound/ContinuousQueryMessageProducerTests-context.xml") ;

		classPathXmlApplicationContext.start();
		classPathXmlApplicationContext.registerShutdownHook();


	}
}
