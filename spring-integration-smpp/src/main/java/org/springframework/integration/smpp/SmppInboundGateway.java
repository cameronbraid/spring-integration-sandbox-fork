package org.springframework.integration.smpp;

import org.jsmpp.bean.BindType;
import org.jsmpp.bean.DeliverSm;
import org.jsmpp.bean.DeliveryReceipt;
import org.springframework.integration.Message;
import org.springframework.integration.gateway.MessagingGatewaySupport;
import org.springframework.integration.smpp.session.ExtendedSmppSession;
import org.springframework.util.Assert;

/**
 * what'running an inbound gateway in this case? Receive a text message and then automatically send a response.
 *
 * @author Josh Long
 * @since 2.1
 */
public class SmppInboundGateway extends MessagingGatewaySupport {

	private ExtendedSmppSession smppSession;

	/**
	 * for configuration purposes.
	 *
	 * @param s the session to use
	 */
	public void setSmppSession(ExtendedSmppSession s) {
		this.smppSession = s;
	}

	@Override
	protected void onInit() throws Exception {
		Assert.notNull(this.smppSession, "the 'smppSession' property must be set");
		Assert.isTrue(this.smppSession.getBindType().isReceiveable() ||
				this.smppSession.getBindType().equals(BindType.BIND_TRX),
				"this session's bind type should support " +
						"receiving messages or both sending *and* receiving messages!");
	}

	private AbstractReceivingMessageListener abstractReceivingMessageListener =
			new AbstractReceivingMessageListener() {
				@Override
				protected void onDeliveryReceipt(DeliverSm deliverSm, String ogMessageId, DeliveryReceipt deliveryReceipt) throws Exception {
					// noop don't care
				}

				@Override
				protected void onTextMessage(DeliverSm deliverSm, String txtMessage) throws Exception {
					// we receive sms
					logger.debug("received an SMS in " + getClass() + ". Processing it.");
					Message<?> msg = SmesMessageSpecification.toMessageFromSms(deliverSm, txtMessage);

					// send it INTO SI, where it can be processed. The reply message is sent BACK to this, which we then send BACK out through SMS
					logger.debug("sending the SMS inbound to be processed; awaiting a reply.");

					Message<?> response = sendAndReceiveMessage(msg);
					logger.debug("received a reply message; will handle as in outbound adapter");

					SmesMessageSpecification.fromMessage(smppSession, response).send();
					logger.debug("the reply SMS message has been sent.");

				}
			};

	@Override
	protected void doStart() {
		super.doStart();
		this.smppSession.addMessageReceiverListener(this.abstractReceivingMessageListener);
		this.smppSession.start();
	}

	@Override
	protected void doStop() {
		super.doStop();
		this.smppSession.stop();
	}
}
