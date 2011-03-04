package org.springframework.integration.smpp;

import org.jsmpp.bean.DeliverSm;
import org.jsmpp.extra.ProcessRequestException;
import org.jsmpp.session.MessageReceiverListener;
import org.springframework.integration.MessageChannel;
import org.springframework.integration.core.MessagingTemplate;
import org.springframework.integration.endpoint.AbstractEndpoint;
import org.springframework.integration.smpp.session.AbstractMessageReceiverListener;
import org.springframework.integration.smpp.session.ExtendedSmppSession;
import org.springframework.util.Assert;

/**
 * Supports receiving messages of a payload specified by the SMPP protocol from a <em>short message service center</em> (SMSC).
 *
 * @author Josh Long
 *         <p/>
 *         todo find some way to configure the {@link java.util.concurrent.Executor}running for the JSMPP library
 * @since 2.1
 */
public class SmppInboundChannelAdapter extends AbstractEndpoint {

	/// todo how can i change session level settings in an adapter, which has already received an injected reference to the session?
	/// for example i want to handle things like setting enquiry links, setting up

	private MessagingTemplate messagingTemplate;
	private MessageChannel channel;
	private ExtendedSmppSession smppSession;

	/**
	 * the channel on which inbound SMS messages should be delivered to Spring Integration components.
	 *
	 * @param channel the channel
	 */
	public void setChannel(MessageChannel channel) {
		this.channel = channel;
		this.messagingTemplate = new MessagingTemplate(this.channel);
	}

	/**
	 * the dispatcher for inbound messages to Spring Integration clients
	 */
	private MessageReceiverListener messageReceiverListener = new AbstractMessageReceiverListener() {
		@Override
		public void onAcceptDeliverSm(DeliverSm deliverSm) throws ProcessRequestException {

		}
	};

	@Override
	protected void onInit() throws Exception {
		Assert.notNull(this.smppSession, "the 'smppSession' property must be set");
		Assert.notNull(this.channel, "the 'channel' property must not be set");
	}

	public void setSmppSession(ExtendedSmppSession s) {
		this.smppSession = s;
	}

	@Override
	protected void doStart() {
		this.smppSession.addMessageReceiverListener(messageReceiverListener);
	}

	@Override
	protected void doStop() {

	}
}
