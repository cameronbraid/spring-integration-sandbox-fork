package org.springframework.integration.smpp;

import org.jsmpp.bean.BindType;
import org.jsmpp.bean.DeliverSm;
import org.jsmpp.bean.DeliveryReceipt;
import org.springframework.integration.Message;
import org.springframework.integration.MessageChannel;
import org.springframework.integration.core.MessagingTemplate;
import org.springframework.integration.endpoint.AbstractEndpoint;
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

	@Override
	protected void onInit() throws Exception {
		Assert.notNull(this.channel, "the 'channel' property must not be set");
		Assert.notNull(this.smppSession, "the 'smppSession' property must be set");
		Assert.isTrue(this.smppSession.getBindType().isReceiveable() ||
								this.smppSession.getBindType().equals(BindType.BIND_TRX),
						"this session's bind type should support " +
						"receiving messages or both sending *and* receiving messages!");
	}

	public void setSmppSession(ExtendedSmppSession s) {
		this.smppSession = s;
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
					Message<?> msg = SmesMessageSpecification.toMessageFromSms(deliverSm ,txtMessage);

					// send it INTO SI, where it can be processed. The reply message is sent BACK to this, which we then send BACK out through SMS
					Message<?> response =	messagingTemplate.sendAndReceive(msg);
					SmesMessageSpecification.fromMessage(smppSession,response).send();
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
