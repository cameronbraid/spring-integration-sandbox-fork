package org.springframework.integration.nativefs;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.integration.Message;
import org.springframework.integration.MessageChannel;
import org.springframework.integration.core.MessagingTemplate;
import org.springframework.integration.support.MessageBuilder;


public class BasicEventDrivenInboundChannelAdapter implements InitializingBean {

	private MessagingTemplate template = new MessagingTemplate();

	private MessageChannel channel;

	public void setChannel(MessageChannel channel) {
		this.channel = channel;
	}

	@Override
	public void afterPropertiesSet() throws Exception {

		Message<?> msg = MessageBuilder.withPayload("hello, world!").build();
		this.template.send(this.channel, msg);

	}
}
