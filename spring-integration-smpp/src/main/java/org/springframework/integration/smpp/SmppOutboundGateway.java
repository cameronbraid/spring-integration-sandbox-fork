package org.springframework.integration.smpp;

import org.springframework.integration.Message;
import org.springframework.integration.handler.AbstractReplyProducingMessageHandler;

/**
 * Support for request/reply exchanges over SMPP to a SMSC.
 * <p/>
 * The request is an outbound SMS message, as in the {@link SmppOutboundChannelAdapter},
 * and the reply can either be the messageId of the outbound message which can ultimately be used to track the confirmation,
 * or the confirmation of the receipt of the outbound message itself. In the latter case, this class simply does the work
 * of waiting for the reply and corellating it to the outbound request.
 * <p/>
 * By default this component assumes one {@link org.jsmpp.session.SMPPSession} in "transceiver" mode - it can both request and reply.
 * Conceptually it should be possible to support two {@link org.jsmpp.session.SMPPSession}s, one in "sender" mode, and another in
 * "receiver" mode and handle the duplexing manually. The corellation logic is the same, in any event.
 * <p/>
 * TODO all of the above
 * TODO also where do we store the correllation data (e.g., it might take ten seconds to get the confirmation and its asynchronous. we need to store it)
 *
 * @author Josh Long
 * @since 2.1
 */
public class SmppOutboundGateway extends AbstractReplyProducingMessageHandler {
	@Override
	protected Object handleRequestMessage(Message<?> requestMessage) {
		return null;
	}
}
