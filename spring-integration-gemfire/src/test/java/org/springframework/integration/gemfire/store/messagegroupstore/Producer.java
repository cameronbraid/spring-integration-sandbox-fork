package org.springframework.integration.gemfire.store.messagegroupstore;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.integration.MessageChannel;
import org.springframework.integration.core.MessagingTemplate;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Collection;

/**
 * Simple endpoint that we can use to send in a lot of test messages
 *
 * @author Josh Long
 */

@Component
public class Producer {

	private MessagingTemplate messagingTemplate = new MessagingTemplate();

	@Value("#{i}") private MessageChannel messageChannel;

	@PostConstruct
	public void start() throws Throwable {
		this.messagingTemplate.setDefaultChannel(this.messageChannel);
	}

	public void sendManyMessages(Collection<String> lines) throws Throwable {
		
		for (String l : lines)
			this.messagingTemplate.send(MessageBuilder.withPayload(l).build());

	}

}
