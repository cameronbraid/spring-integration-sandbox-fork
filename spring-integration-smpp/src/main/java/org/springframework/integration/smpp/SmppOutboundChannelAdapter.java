package org.springframework.integration.smpp;

import org.jsmpp.bean.BindType;
import org.jsmpp.bean.TypeOfNumber;
import org.jsmpp.util.AbsoluteTimeFormatter;
import org.jsmpp.util.TimeFormatter;
import org.springframework.context.Lifecycle;
import org.springframework.integration.Message;
import org.springframework.integration.MessagingException;
import org.springframework.integration.context.IntegrationObjectSupport;
import org.springframework.integration.core.MessageHandler;
import org.springframework.integration.smpp.session.ExtendedSmppSession;
import org.springframework.integration.smpp.session.ExtendedSmppSessionAdaptingDelegate;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Sends messages to an SMS gateway using SMPP. Most of the work in terms of converting inbound messsage headers
 * (whose keys, by the way, live in {@link SmppConstants}) is done by {@link SmesMessageSpecification}, which
 * handles <em>all</em> the tedium of converting and validating the configuration.
 * <p/>
 * This adapter supports  <em>mobile terminated (MT)</em> messaging, where the recipient is a directory phone number.
 *
 * @author Josh Long
 * @since 2.1
 */
public class SmppOutboundChannelAdapter extends IntegrationObjectSupport implements MessageHandler {

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

	@Override
	protected void onInit() throws Exception {
		if (this.timeFormatter == null) {
			this.timeFormatter = new AbsoluteTimeFormatter();
		}

		Assert.notNull(this.smppSession, "the smppSession must not be null");
		Assert.isTrue(!this.smppSession.getBindType().equals(BindType.BIND_RX),
				"the BindType must support message production: BindType.TX or BindType.TRX only supported");

		this.smppSession.start();

	}

	private SmesMessageSpecification applyDefaultsIfNecessary(SmesMessageSpecification smsSpec) {

		if (defaultSourceAddressTypeOfNumber != null)
			smsSpec.setSourceAddressTypeOfNumberIfRequired(this.defaultSourceAddressTypeOfNumber);

		if (StringUtils.hasText(this.defaultSourceAddress))
			smsSpec.setSourceAddressIfRequired(this.defaultSourceAddress);

		return smsSpec;
	}

	public void setSmppSession(ExtendedSmppSession s) {
		this.smppSession = s;
	}

	public void handleMessage(Message<?> message) throws MessagingException {

		try {
			// todo support a gateway and have that gateway also handle message delivery receipt notifications
			// that will correlate this smsMessageId with the ID that comes back asynchronously from the SMSC indicating that
			// the message has been delivered.
			// this could require that we keep a correlation map since its possible upstream SMSC
			// unused return value -- see gateway

			SmesMessageSpecification specification = applyDefaultsIfNecessary(
					SmesMessageSpecification.fromMessage(this.smppSession, message)
							.setTimeFormatter(this.timeFormatter));

			String smsMessageId = specification.send();
			logger.debug( "sent message : "+message.getPayload());
			logger.debug("message ID for the sent message is: " + smsMessageId);
		} catch (Exception e) {
			throw new RuntimeException("Exception in trying to process the inbound SMPP message", e);
		}
	}
}
