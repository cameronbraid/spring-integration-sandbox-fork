package org.springframework.integration.gemfire.store.messagegroupstore;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.integration.Message;
import org.springframework.integration.MessageChannel;
import org.springframework.integration.core.MessagingTemplate;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

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

	@Value("${correlation-header}" ) private String correlationHeader ;

	@PostConstruct
	public void start() throws Throwable {
		this.messagingTemplate.setDefaultChannel(this.messageChannel);
	}

	/**
	 *
	 * @param lines
	 * @throws Throwable
	 */
	public void sendManyMessages(int correlationValue, Collection<String> lines) throws Throwable {
		Assert.notNull( lines, "the collection must be non-null");
		Assert.notEmpty( lines, "the collection must not be empty");

		int ctr = 0,
			size = lines.size() ;

		for (String l : lines){
			Message<?> msg = MessageBuilder.withPayload(l)
					.setCorrelationId( this.correlationHeader)
				    .setHeader( this.correlationHeader , correlationValue )
					.setSequenceNumber(++ctr)
					.setSequenceSize( size)
					.build();

			this.messagingTemplate.send( msg );

		}
	}

}
