package org.springframework.integration.gemfire.store.messagegroupstore;

import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.util.Arrays;


/**
 * simple example demonstrating the use of a {@link org.springframework.integration.gemfire.store.GemfireMessageGroupStore}
 */
public class Main {
    public static void main(String[] args) throws Throwable {

		ClassPathXmlApplicationContext classPathXmlApplicationContext = new ClassPathXmlApplicationContext(
                "/org/springframework/integration/gemfire/store/messagegroupstore/GemfireMessageStore-context.xml");

		Producer producer = classPathXmlApplicationContext.getBean(Producer.class);

		for(int i =0 ;  i < 10 ; i++ ) // each iteration will create a new, unique MessageGroup instance in the MessageGroupStore 
			producer.sendManyMessages(  i, Arrays.asList("1,2,3,4,5".split(",")));


		Thread.sleep( 1000 * 10 );
    }
}
