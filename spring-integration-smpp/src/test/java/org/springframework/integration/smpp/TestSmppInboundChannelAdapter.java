package org.springframework.integration.smpp;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.integration.Message;
import org.springframework.integration.MessagingException;
import org.springframework.integration.core.MessageHandler;
import org.springframework.integration.core.SubscribableChannel;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@ContextConfiguration("classpath:TestSmppInboundChannelAdapter-context.xml")
@RunWith(SpringJUnit4ClassRunner.class)
public class TestSmppInboundChannelAdapter {

	@Autowired @Qualifier("inbound")
	SubscribableChannel messageChannel;

	@Autowired
	ApplicationContext context ;

	@Test
	public void testReceiving () throws Throwable {
		messageChannel.subscribe(new MessageHandler(){
			public void handleMessage(Message<?> message) throws MessagingException {
			 System.out.println(message.getPayload().toString())  ;
			}
		});
		Thread.sleep(1000 * 10);

	/*	ConsumerEndpointFactoryBean consumerEndpointFactoryBean =
				new ConsumerEndpointFactoryBean();
		consumerEndpointFactoryBean.setInputChannel(this.messageChannel);
		consumerEndpointFactoryBean.setBeanFactory( context);
		consumerEndpointFactoryBean.setHandler( new MessageHandler(){
			public void handleMessage(Message<?> message) throws MessagingException {
			 System.out.println(message.getPayload().toString())  ;
			}
		});

		AbstractEndpoint abstractEndpoint =
				consumerEndpointFactoryBean.getObject();

		this.messageChannel.

		abstractEndpoint.start();*/

	}
}
