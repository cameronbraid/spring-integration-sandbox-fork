package org.springframework.integration.smpp;

import org.jsmpp.session.SMPPSession;
import org.jsmpp.util.AbsoluteTimeFormatter;
import org.jsmpp.util.TimeFormatter;
import org.springframework.integration.Message;
import org.springframework.integration.MessagingException;
import org.springframework.integration.context.IntegrationObjectSupport;
import org.springframework.integration.core.MessageHandler;
import org.springframework.util.Assert;

/**
 * Sends messages to an SMS gateway using SMPP. Most of the work in terms of converting inbound messsage headers
 * (whose keys, by the way, live in {@link SmppConstants}) is done by {@link SmesMessageSpecification}, which
 * handles <em>all</em> the tedium of converting and validating the configuration.
 *
 * @author Josh Long
 * @since 2.1
 */
public class SmppOutboundChannelAdapter extends IntegrationObjectSupport implements MessageHandler {

	private TimeFormatter timeFormatter;

	private SMPPSession smppSession;

	/**
	 * the default is {@link AbsoluteTimeFormatter}, though there is also a {@link org.jsmpp.util.RelativeTimeFormatter}
	 *
	 * @param timeFormatter the time formatter to use.
	 */
	public void setTimeFormatter(TimeFormatter timeFormatter) {
		this.timeFormatter = timeFormatter;
	}

	@Override
	protected void onInit() throws Exception {
		Assert.notNull(this.smppSession, "the smppSession must not be null");
		if (this.timeFormatter == null) {
			this.timeFormatter = new AbsoluteTimeFormatter();
		}
	}

	public void setSmppSession(SMPPSession smppSession) {
		this.smppSession = smppSession;
	}

	@Override
	public void handleMessage(Message<?> message) throws MessagingException {

		try {

			// todo support a gateway and have that gateway also handle message delivery receipt notifications
			// that will correlate this msgId with the ID that comes back asynchronously from the SMSC indicating that
			// the message has been delivered.
			// this could require that we keep a correllation map since its possible upstream SMSC

			String msgId = SmesMessageSpecification.fromMessage(this.smppSession, message)
					.setTimeFormatter(this.timeFormatter)
					.send();
		} catch (Exception e) {
			throw new RuntimeException("Exception in trying to process the inbound SMPP message", e);
		}
	}
}
