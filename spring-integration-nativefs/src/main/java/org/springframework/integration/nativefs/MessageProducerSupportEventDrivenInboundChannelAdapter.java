package org.springframework.integration.nativefs;

import org.springframework.integration.Message;
import org.springframework.integration.endpoint.MessageProducerSupport;
import org.springframework.integration.support.MessageBuilder;

public class MessageProducerSupportEventDrivenInboundChannelAdapter extends MessageProducerSupport {

	@Override
	protected void doStart() {
		Message<?> msg = MessageBuilder.withPayload("hello, world!").build();
		this.sendMessage(msg);
	}
}
