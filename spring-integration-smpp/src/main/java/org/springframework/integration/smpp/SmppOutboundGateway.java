package org.springframework.integration.smpp;

import org.jsmpp.bean.TypeOfNumber;
import org.jsmpp.session.ClientSession;
import org.jsmpp.util.AbsoluteTimeFormatter;
import org.jsmpp.util.TimeFormatter;
import org.springframework.integration.Message;
import org.springframework.integration.handler.AbstractReplyProducingMessageHandler;
import org.springframework.integration.smpp.session.ExtendedSmppSession;
import org.springframework.integration.smpp.session.ExtendedSmppSessionAdaptingDelegate;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.util.StringUtils;

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
 *
 * @author Josh Long
 * @since 2.1
 */
public class SmppOutboundGateway extends AbstractReplyProducingMessageHandler {
	@Override
	protected Object handleRequestMessage(Message<?> requestMessage) {
		try {


			SmesMessageSpecification specification = applyDefaultsIfNecessary(
					SmesMessageSpecification.fromMessage(this.smppSession, requestMessage)
							.setTimeFormatter(this.timeFormatter));

			String smsMessageId = specification.send();

			logger.debug("message ID for the sent message is: " + smsMessageId);

			return MessageBuilder.withPayload(smsMessageId).build()  ;

		} catch (Exception e) {
			throw new RuntimeException("Exception in trying to process the inbound SMPP message", e);
		}
	}

	private String defaultSourceAddress;

	private TypeOfNumber defaultSourceAddressTypeOfNumber = TypeOfNumber.UNKNOWN;

	private TimeFormatter timeFormatter = new AbsoluteTimeFormatter();

	private ExtendedSmppSession smppSession;

	@SuppressWarnings("unused")
	public void setDefaultSourceAddress(String defaultSourceAddress) {
		this.defaultSourceAddress = defaultSourceAddress;
	}

	@SuppressWarnings("unused")
	public void setDefaultSourceAddressTypeOfNumber(TypeOfNumber defaultSourceAddressTypeOfNumber) {
		this.defaultSourceAddressTypeOfNumber = defaultSourceAddressTypeOfNumber;
	}

	@SuppressWarnings("unused")
	public void setTimeFormatter(TimeFormatter timeFormatter) {
		this.timeFormatter = timeFormatter;
	}



	private SmesMessageSpecification applyDefaultsIfNecessary(SmesMessageSpecification smsSpec) {

		if (defaultSourceAddressTypeOfNumber != null)
			smsSpec.setSourceAddressTypeOfNumberIfRequired(this.defaultSourceAddressTypeOfNumber);

		if (StringUtils.hasText(this.defaultSourceAddress))
			smsSpec.setSourceAddressIfRequired(this.defaultSourceAddress);

		return smsSpec;
	}

	public void setSmppSession(ClientSession s) {

		if (!(s instanceof ExtendedSmppSession))
			this.smppSession = new ExtendedSmppSessionAdaptingDelegate(s);
		else {
			this.smppSession = (ExtendedSmppSession) s;
		}
	}

}
