package org.springframework.integration.smpp;

import org.jsmpp.bean.TypeOfNumber;
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
 * <p/>
 * This adapter supports  <em>mobile terminated (MT)</em> messaging, where the recipient is a directory phone number.
 *
 * @author Josh Long
 * @since 2.1
 */
public class SmppOutboundChannelAdapter extends IntegrationObjectSupport implements MessageHandler {

	/**
	 * the number from which the message is sent. This will be used if there is no corresponding value in the inbound message headers.
	 *
	 * @param defaultSourceAddress the source phone number.
	 */
	public void setDefaultSourceAddress(String defaultSourceAddress) {
		this.defaultSourceAddress = defaultSourceAddress;
	}

	/**
	 * the phone number from which messages are sent
	 *
	 * @param defaultSourceTypeOfAddress the phone number from which messages are sent.
	 */
	public void setDefaultSourceTypeOfAddress(TypeOfNumber defaultSourceTypeOfAddress) {
		this.defaultSourceTypeOfAddress = defaultSourceTypeOfAddress;
	}

	private String defaultSourceAddress;

	private TypeOfNumber defaultSourceTypeOfAddress = TypeOfNumber.UNKNOWN;

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
/*

	private SmesMessageSpecification  applyDefaultsIfNecessary(SmesMessageSpecification smsSpec ){
		// the sourceTON
		Assert.isTrue( !(this.defaultSourceTypeOfAddress == null && smsSpec.getSourceAddressTypeOfNumber() ==null ),
				"there is no applicable, valid value for the source address type of number" );

		if(!StringUtils.hasText( smsSpec.getSourceAddress()) &&StringUtils.hasText(this.defaultSourceAddress ) ){
			smsSpec.setSourceAddress( this.defaultSourceAddress );
		}

		// the sourceAddress
		if(smsSpec.getSourceAddressTypeOfNumber()==null && this.defaultSourceTypeOfAddress != null )
			smsSpec.setSourceAddressTypeOfNumber( this.defaultSourceTypeOfAddress) ;

		Assert.isTrue(StringUtils.hasText(this.defaultSourceAddress) || StringUtils.hasText(smsSpec.getSourceAddress()),
				"there is no applicable, valid value for the source address.");

		return smsSpec ;

	}
*/

	public void setSmppSession(SMPPSession smppSession) {
		this.smppSession = smppSession;
	}

	@Override
	public void handleMessage(Message<?> message) throws MessagingException {

		try {

			// todo support a gateway and have that gateway also handle message delivery receipt notifications
			// that will correlate this smsMessageId with the ID that comes back asynchronously from the SMSC indicating that
			// the message has been delivered.
			// this could require that we keep a correlation map since its possible upstream SMSC

			// unused return value -- see gateway
			SmesMessageSpecification specification = (
					SmesMessageSpecification.fromMessage(this.smppSession, message)
							.setTimeFormatter(this.timeFormatter));

			String smsMessageId = specification.send();

			logger.debug("message ID for the sent message is: " + smsMessageId);
		} catch (Exception e) {
			throw new RuntimeException("Exception in trying to process the inbound SMPP message", e);
		}
	}
}
