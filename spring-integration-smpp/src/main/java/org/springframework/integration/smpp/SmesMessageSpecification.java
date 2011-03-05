package org.springframework.integration.smpp;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jsmpp.bean.*;
import org.jsmpp.session.ClientSession;
import org.jsmpp.session.SMPPSession;
import org.jsmpp.util.AbsoluteTimeFormatter;
import org.jsmpp.util.TimeFormatter;
import org.springframework.integration.Message;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.util.Date;

import static org.springframework.integration.smpp.SmppConstants.*;

/**
 * fluent API to help make specifying all these parameters just a <em>tiny</em> bit easier. For internal use only.
 *
 * @author Josh Long
 * @since 2.1
 */
public class SmesMessageSpecification {

	private Log log = LogFactory.getLog(getClass());
	private TimeFormatter timeFormatter = new AbsoluteTimeFormatter();

	private int maxLengthSmsMessages = 140;
	private String sourceAddress;
	private String destinationAddress;
	private String serviceType;
	private TypeOfNumber sourceAddressTypeOfNumber;
	private NumberingPlanIndicator sourceAddressNumberingPlanIndicator;
	private TypeOfNumber destinationAddressTypeOfNumber;
	private NumberingPlanIndicator destinationAddressNumberingPlanIndicator;
	private ESMClass esmClass;
	private byte protocolId;
	private byte priorityFlag;
	private String scheduleDeliveryTime = timeFormatter.format(new Date());
	private String validityPeriod;
	private RegisteredDelivery registeredDelivery;
	private byte replaceIfPresentFlag;
	private DataCoding dataCoding;
	private byte smDefaultMsgId;
	private byte[] shortMessage;
	private ClientSession smppSession;

	/**
	 * this method takes an inbound SMS message and converts it to a Spring Integration message
	 *
	 * @param dsm				the {@link DeliverSm} from {@link AbstractReceivingMessageListener#onTextMessage(org.jsmpp.bean.DeliverSm, String)}
	 * @param txtMessage the String from {@link AbstractReceivingMessageListener#onTextMessage(org.jsmpp.bean.DeliverSm, String)}
	 * @return a Spring Integration message
	 */
	public static Message<?> toMessageFromSms(DeliverSm dsm, String txtMessage) {

		Assert.isTrue(!dsm.isSmscDeliveryReceipt(), "the message should not be a delivery confirmation receipt!");

		MessageBuilder<String> mb = MessageBuilder.withPayload(txtMessage);
		mb.setHeader(SmppConstants.SMS, dsm);
		mb.setHeader(SmppConstants.REPLACE_IF_PRESENT, dsm.getReplaceIfPresent());
		mb.setHeader(SmppConstants.SHORT_MESSAGE, dsm.getShortMessage());
		mb.setHeader(SmppConstants.OPTIONAL_PARAMETES, dsm.getOptionalParametes());
		mb.setHeader(SmppConstants.UDHI_AND_REPLY_PATH, dsm.isUdhiAndReplyPath());
		mb.setHeader(SmppConstants.VALIDITY_PERIOD, dsm.getValidityPeriod());
		mb.setHeader(SmppConstants.COMMAND_LENGTH, dsm.getCommandLength());
		mb.setHeader(SmppConstants.COMMAND_ID, dsm.getCommandId());
		mb.setHeader(SmppConstants.SME_ACK_NOT_REQUESTED, dsm.isSmeAckNotRequested());
		mb.setHeader(SmppConstants.DATA_CODING, dsm.getDataCoding());
		mb.setHeader(SmppConstants.REPLY_PATH, dsm.isReplyPath());
		mb.setHeader(SmppConstants.SOURCE_ADDR_TON, dsm.getSourceAddrTon());
		mb.setHeader(SmppConstants.SM_DEFAULT_MSG_ID, dsm.getSmDefaultMsgId());
		mb.setHeader(SmppConstants.UDHI, dsm.isUdhi());
		mb.setHeader(SmppConstants.SME_MANUAL_ACKNOWLEDGMENT, dsm.isSmeManualAcknowledgment());
		mb.setHeader(SmppConstants.CONVERSATION_ABORT, dsm.isConversationAbort());
		mb.setHeader(SmppConstants.DEST_ADDRESS, dsm.getDestAddress());
		mb.setHeader(SmppConstants.ESM_CLASS, dsm.getEsmClass());
		mb.setHeader(SmppConstants.COMMAND_ID_AS_HEX, dsm.getCommandIdAsHex());
		mb.setHeader(SmppConstants.SME_DELIVERY_AND_MANUAL_ACK_REQUESTED, dsm.isSmeDeliveryAndManualAckRequested());
		mb.setHeader(SmppConstants.SMSC_DELIVERY_RECEIPT, dsm.isSmscDeliveryReceipt());
		mb.setHeader(SmppConstants.SME_MANUAL_ACK_REQUESTED, dsm.isSmeManualAckRequested());
		mb.setHeader(SmppConstants.PRIORITY_FLAG, dsm.getPriorityFlag());
		mb.setHeader(SmppConstants.DEST_ADDR_TON, dsm.getDestAddrTon());
		mb.setHeader(SmppConstants.COMMAND_STATUS_AS_HEX, dsm.getCommandStatusAsHex());
		mb.setHeader(SmppConstants.SERVICE_TYPE, dsm.getServiceType());
		mb.setHeader(SmppConstants.INTERMEDIATE_DELIVERY_NOTIFICATION, dsm.isIntermedietDeliveryNotification());
		mb.setHeader(SmppConstants.SOURCE_ADDR_NPI, dsm.getSourceAddrNpi());
		mb.setHeader(SmppConstants.REGISTERED_DELIVERY, dsm.getRegisteredDelivery());
		mb.setHeader(SmppConstants.DEST_ADDR_NPI, dsm.getDestAddrNpi());
		mb.setHeader(SmppConstants.COMMAND_STATUS, dsm.getCommandStatus());
		mb.setHeader(SmppConstants.DEFAULT_MESSAGE_TYPE, dsm.isDefaultMessageType());
		mb.setHeader(SmppConstants.PROTOCOL_ID, dsm.getProtocolId());
		mb.setHeader(SmppConstants.SOURCE_ADDR, dsm.getSourceAddr());
		mb.setHeader(SmppConstants.SEQUENCE_NUMBER, dsm.getSequenceNumber());
		mb.setHeader(SmppConstants.SCHEDULE_DELIVERY_TIME, dsm.getScheduleDeliveryTime());
		mb.setHeader(SmppConstants.SME_DELIVERY_ACK_REQUESTED, dsm.isSmeDeliveryAckRequested());
		return mb.build();
	}

	/**
	 * this method will take an inbound Spring Integration {@link Message} and map it to a {@link SmesMessageSpecification}
	 * which we can use to send the SMS message.
	 *
	 * @param msg				 a new {@link Message}
	 * @param smppSession the SMPPSession
	 * @return a {@link SmesMessageSpecification}
	 */
	public static SmesMessageSpecification fromMessage(ClientSession smppSession, Message<?> msg) {

		String srcAddy = valueIfHeaderExists(SRC_ADDR, msg);
		String dstAddy = valueIfHeaderExists(DST_ADDR, msg);
		String smsTxt = valueIfHeaderExists(SMS_MSG, msg);
		if (!StringUtils.hasText(smsTxt)) {
			Object payload = msg.getPayload();
			if (payload instanceof String) {
				smsTxt = (String) payload;
			}
		}
		SmesMessageSpecification spec = SmesMessageSpecification.newSmesMessageSpecification(smppSession, srcAddy, dstAddy, smsTxt);
		spec.setDestinationAddressNumberingPlanIndicator(SmesMessageSpecification.<NumberingPlanIndicator>valueIfHeaderExists(DST_NPI, msg));
		spec.setSourceAddressNumberingPlanIndicator(SmesMessageSpecification.<NumberingPlanIndicator>valueIfHeaderExists(SRC_NPI, msg));
		spec.setDestinationAddressTypeOfNumber(SmesMessageSpecification.<TypeOfNumber>valueIfHeaderExists(DST_TON, msg));
		spec.setSourceAddressTypeOfNumber(SmesMessageSpecification.<TypeOfNumber>valueIfHeaderExists(SRC_TON, msg));
		spec.setServiceType(SmesMessageSpecification.<String>valueIfHeaderExists(SERVICE_TYPE, msg));
		spec.setEsmClass(SmesMessageSpecification.esmClassFromHeader(msg));
		spec.setScheduleDeliveryTime(SmesMessageSpecification.<Date>valueIfHeaderExists(SCHEDULED_DELIVERY_TIME, msg));
		spec.setDataCoding(SmesMessageSpecification. dataCodingFromHeader( msg));
		spec.setValidityPeriod(SmesMessageSpecification.<String>valueIfHeaderExists(VALIDITY_PERIOD, msg));

		// byte landmine. autoboxing causes havoc with <em>null</em> bytes.
		Byte priorityFlag1 = SmesMessageSpecification.<Byte>valueIfHeaderExists(PRIORITY_FLAG, msg);
		if (priorityFlag1 != null)
			spec.setPriorityFlag(priorityFlag1);

		Byte smDefaultMsgId1 = SmesMessageSpecification.<Byte>valueIfHeaderExists(SM_DEFAULT_MSG_ID, msg);
		if (smDefaultMsgId1 != null)
			spec.setSmDefaultMsgId(smDefaultMsgId1);

		Byte replaceIfPresentFlag1 = SmesMessageSpecification.<Byte>valueIfHeaderExists(REPLACE_IF_PRESENT_FLAG, msg);
		if (replaceIfPresentFlag1 != null)
			spec.setReplaceIfPresentFlag(replaceIfPresentFlag1);

		Byte protocolId1 = SmesMessageSpecification.<Byte>valueIfHeaderExists(PROTOCOL_ID, msg);
		if (null != protocolId1)
			spec.setProtocolId(protocolId1);

		spec.setRegisteredDelivery(registeredDeliveryFromHeader(msg));

		return spec;
	}

	private static DataCoding dataCodingFromHeader( Message<?> msg) {
		Object dc = msg.getHeaders().get(DATA_CODING);
		if(dc instanceof DataCoding){
			return (DataCoding)dc ;
		}
		if( dc instanceof  Byte){
			return DataCoding.newInstance((Byte)dc);

		}

		return null ;
	}

	/**
	 * need to be a little flexibile about what we take in as {@link SmppConstants#REGISTERED_DELIVERY_MODE}. The value can
	 * be a String or a member of the {@link SMSCDeliveryReceipt} enum.
	 *
	 * @param msg the Spring Integration message
	 * @return a value for {@link RegisteredDelivery} or null, which is good because it'll simply let the existing default work
	 */
	private static RegisteredDelivery registeredDeliveryFromHeader(Message<?> msg) {
		Object rd = valueIfHeaderExists(REGISTERED_DELIVERY_MODE, msg);

		if (rd instanceof String) {
			String rdString = (String) rd;
			SMSCDeliveryReceipt smscDeliveryReceipt = SMSCDeliveryReceipt.valueOf(rdString);
			Assert.notNull(smscDeliveryReceipt, "the registeredDelivery can't be null");
			return new RegisteredDelivery(smscDeliveryReceipt);
		}

		if (rd instanceof SMSCDeliveryReceipt) {
			SMSCDeliveryReceipt smscDeliveryReceipt = (SMSCDeliveryReceipt) rd;
			return new RegisteredDelivery(smscDeliveryReceipt);
		}

		if (rd instanceof RegisteredDelivery) {
			return (RegisteredDelivery) rd;
		}
		return null;
	}

	/**
	 * you need to use the builder API
	 *
	 * @param smppSession the SMPPSession instance against which we should work.
	 * @see org.springframework.integration.smpp.SmesMessageSpecification#SmesMessageSpecification()
	 */

	@SuppressWarnings("unused")
	SmesMessageSpecification(SMPPSession smppSession) {
		this.smppSession = smppSession;
	}

	/**
	 * tries to safely extract the ESMClass
	 * @param im
	 * @return
	 */
	static private  ESMClass esmClassFromHeader( Message<?> im){
		String h = ESM_CLASS ;
		Object o = valueIfHeaderExists(h,im);
		ESMClass response = null ;
		if(o instanceof  Byte){
			response = new ESMClass((Byte)o);

		}
		else if(o instanceof ESMClass){
			response = (ESMClass)o;
		}
		return response;
	}

	@SuppressWarnings("unchecked")
	static private <T> T valueIfHeaderExists(String h, Message<?> msg) {
		if (msg != null && msg.getHeaders().containsKey(h))
			return (T) msg.getHeaders().get(h);
		return null;
	}

	/**
	 * Everybody else has to use the builder API. DO NOT make this private or it will not be proxied and that will make me sad!
	 *
	 * @param ss the {@link SMPPSession}
	 * @return the current spec
	 */
	SmesMessageSpecification setSmppSession(ClientSession ss) {
		this.smppSession = ss;
		return this;
	}

	/**
	 * use the builder API, but we need this to cleanly proxy
	 */
	@SuppressWarnings("unused")
	SmesMessageSpecification() {
		this(null);
	}

	/**
	 * Conceptually, you could get away with just specifying these three parameters, though I don't know how likely that is in practice.
	 *
	 * @param srcAddress	the source address
	 * @param destAddress the destination address
	 * @param txtMessage	the message to send (must be  no more than 140 characters
	 * @param ss					the SMPPSession
	 * @return the {@link SmesMessageSpecification}
	 */
	public static SmesMessageSpecification newSmesMessageSpecification(ClientSession ss, String srcAddress, String destAddress, String txtMessage) {

		SmesMessageSpecification smesMessageSpecification = new SmesMessageSpecification();

		smesMessageSpecification
				.reset()
				.setSmppSession(ss)
				.setSourceAddress(srcAddress)
				.setDestinationAddress(destAddress)
				.setShortTextMessage(txtMessage);

		return smesMessageSpecification;
	}

	/**
	 * Only sets the #sourceAddressTypeOfNumber if the current value is null, otherwise, it leaves it.
	 *
	 * @param sourceAddressTypeOfNumberIfRequired
	 *         the {@link TypeOfNumber}
	 * @return this
	 */
	public SmesMessageSpecification setSourceAddressTypeOfNumberIfRequired(TypeOfNumber sourceAddressTypeOfNumberIfRequired) {
		if (this.sourceAddressTypeOfNumber == null)
			this.sourceAddressTypeOfNumber = sourceAddressTypeOfNumberIfRequired;
		return this;
	}

	/**
	 * send the message on its way.
	 * <p/>
	 * todo can we do something smart here or through an adapter to handle the situation where we have asked for a message receipt? what about if we're using a message receipt <em>and</eM> we're only a receiver or a sender connection and not a transceiver? We need gateway semantics across two unidirectional SMPPSessions, then
	 *
	 * @return the messageId (required if you want to then track it or correllate it with message receipt confirmations)
	 * @throws Exception the {@link SMPPSession#submitShortMessage(String, org.jsmpp.bean.TypeOfNumber, org.jsmpp.bean.NumberingPlanIndicator, String, org.jsmpp.bean.TypeOfNumber, org.jsmpp.bean.NumberingPlanIndicator, String, org.jsmpp.bean.ESMClass, byte, byte, String, String, org.jsmpp.bean.RegisteredDelivery, byte, org.jsmpp.bean.DataCoding, byte, byte[], org.jsmpp.bean.OptionalParameter...)} method throws lots of Exceptions, including {@link java.io.IOException}
	 */
	public String send() throws Exception {
		validate();
		String msgId = this.smppSession.submitShortMessage(
				this.serviceType,
				this.sourceAddressTypeOfNumber,
				this.sourceAddressNumberingPlanIndicator,
				this.sourceAddress,

				this.destinationAddressTypeOfNumber,
				this.destinationAddressNumberingPlanIndicator,
				this.destinationAddress,

				this.esmClass,
				this.protocolId,
				this.priorityFlag,
				this.scheduleDeliveryTime,
				this.validityPeriod,
				this.registeredDelivery,
				this.replaceIfPresentFlag,
				this.dataCoding,
				this.smDefaultMsgId,
				this.shortMessage);

		return msgId;
	}

	protected void validate() {
		Assert.notNull(this.sourceAddress, "the source address must not be null");
		Assert.notNull(this.destinationAddress, "the destination address must not be null");
		Assert.isTrue(this.shortMessage != null && this.shortMessage.length > 0, "the message must not be null");
	}

	public SmesMessageSpecification setSourceAddress(String sourceAddr) {
		if (!nullHeaderWillOverwriteDefault(sourceAddr))
			this.sourceAddress = sourceAddr;
		return this;
	}

	/**
	 * the 'to' phone number
	 *
	 * @param destinationAddr the phone number
	 * @return the current spec
	 */
	public SmesMessageSpecification setDestinationAddress(String destinationAddr) {
		this.destinationAddress = destinationAddr;
		return this;
	}

	public SmesMessageSpecification setServiceType(String serviceType) {
		if (!nullHeaderWillOverwriteDefault(serviceType))
			this.serviceType = serviceType;
		return this;
	}

	public SmesMessageSpecification setSourceAddressTypeOfNumber(TypeOfNumber sourceAddrTon) {
		if (!nullHeaderWillOverwriteDefault(sourceAddrTon))
			this.sourceAddressTypeOfNumber = sourceAddrTon;
		return this;
	}

	public SmesMessageSpecification setSourceAddressNumberingPlanIndicator(NumberingPlanIndicator sourceAddrNpi) {
		if (!nullHeaderWillOverwriteDefault(sourceAddrNpi))
			this.sourceAddressNumberingPlanIndicator = sourceAddrNpi;
		return this;
	}

	public SmesMessageSpecification setDestinationAddressTypeOfNumber(TypeOfNumber destAddrTon) {
		if (!nullHeaderWillOverwriteDefault(destAddrTon))
			this.destinationAddressTypeOfNumber = destAddrTon;
		return this;
	}

	/**
	 * guard against overwriting perfectly good defaults with null values.
	 *
	 * @param v value the value
	 * @return can the write proceed unabated?
	 */
	private boolean nullHeaderWillOverwriteDefault(Object v) {
		if (v == null) {
			if (log.isDebugEnabled()) log.debug("There is a default in place for this property; don't overwrite it with null");
			return true;
		}
		return false;
	}

	public SmesMessageSpecification setDestinationAddressNumberingPlanIndicator(NumberingPlanIndicator destAddrNpi) {
		if (!nullHeaderWillOverwriteDefault(destAddrNpi))
			this.destinationAddressNumberingPlanIndicator = destAddrNpi;
		return this;
	}

	public SmesMessageSpecification setEsmClass(ESMClass esmClass) {
		if (!nullHeaderWillOverwriteDefault(esmClass))
			this.esmClass = esmClass;
		return this;
	}

	public SmesMessageSpecification setProtocolId(byte protocolId) {
		if (!nullHeaderWillOverwriteDefault(protocolId))
			this.protocolId = protocolId;
		return this;
	}

	public SmesMessageSpecification setPriorityFlag(byte pf) {
		if (!nullHeaderWillOverwriteDefault(pf))
			this.priorityFlag = pf;
		return this;
	}

	/**
	 * When you submit a message to an SMSC, it is possible to sometimes specify a
	 * <em>validity period</em> for the message. This setting is an instruction to the SMSC that stipulates that
	 * if the message cannot be delivered to the recipient within the next N minutes or hours or days,
	 * the SMSC should discard the message. This would mean that if the recipient'running mobile phone is
	 * turned off, or outSession of coverage for x minutes/hours/days after the message is submitted, the SMSC
	 * should not perform further delivery retry and should discard the message.
	 * <p/>
	 * Of course, there is no guarantee that the operator SMSC will respect this setting, so it needs
	 * to be tested with a particular operator first to determine if it can be used reliably.
	 * <p/>
	 * That information came from <a href="http://www.nowsms.com/smpp-information">the NowSMS website.</a>.
	 *
	 * @param v the period of validity. There are specific formats for this, however this method provides no validation.
	 *          <p/>
	 *          todo provide format validation if possible
	 * @return the current SmesMessageSpecification
	 */
	public SmesMessageSpecification setValidityPeriod(String v) {
		if (!nullHeaderWillOverwriteDefault(v))
			this.validityPeriod = v;
		return this;
	}

	public SmesMessageSpecification setScheduleDeliveryTime(Date d) {
		if (!nullHeaderWillOverwriteDefault(d))
			this.scheduleDeliveryTime = timeFormatter.format(d);
		return this;
	}

	public SmesMessageSpecification setRegisteredDelivery(RegisteredDelivery rd) {
		if (!nullHeaderWillOverwriteDefault(rd))
			this.registeredDelivery = rd;
		return this;
	}

	public SmesMessageSpecification setReplaceIfPresentFlag(byte replaceIfPresentFlag) {
		if (!nullHeaderWillOverwriteDefault(replaceIfPresentFlag))
			this.replaceIfPresentFlag = replaceIfPresentFlag;
		return this;
	}

	public SmesMessageSpecification setDataCoding(DataCoding dataCoding) {
		if (!nullHeaderWillOverwriteDefault(dataCoding))
			this.dataCoding = dataCoding;
		return this;
	}

	public SmesMessageSpecification setSmDefaultMsgId(byte smDefaultMsgId) {
		this.smDefaultMsgId = smDefaultMsgId;
		return this;
	}

	public SmesMessageSpecification setTimeFormatter(TimeFormatter timeFormatter) {
		if (!nullHeaderWillOverwriteDefault(timeFormatter))
			this.timeFormatter = timeFormatter;
		return this;
	}

	/**
	 * todo it'running not <em>quite</em> true that the payload needs to be 140c. A large message can be split up into smaller messages,
	 * but for now it'running more useful to have this validation in place than not.
	 *
	 * @param s the text message body
	 * @return the SmesMessageSpecification
	 */
	public SmesMessageSpecification setShortTextMessage(String s) {
		Assert.notNull(s, "the SMS message payload must not be null");
		Assert.isTrue(s.length() <= this.maxLengthSmsMessages, "the SMS message payload must be 140 characters or less.");
		this.shortMessage = s.getBytes();
		return this;
	}

	/**
	 * this is a good value, but not strictly speaking universal. This is intended only for exceptional configuration cases
	 * <p/>
	 * See: http://www.nowsms.com/long-sms-text-messages-and-the-160-character-limit
	 *
	 * @param maxLengthSmsMessages the length of sms messages
	 * @see #setShortTextMessage(String)
	 */
	@SuppressWarnings("unused")
	public void setMaxLengthSmsMessages(int maxLengthSmsMessages) {
		this.maxLengthSmsMessages = maxLengthSmsMessages;
	}

	/**
	 * Resets the thread local, pooled objects to a known state before reuse.
	 * <p/>
	 * Resetting the variables is trivially cheap compared to proxying a new one each time.
	 *
	 * @return the cleaned up {@link SmesMessageSpecification}
	 */
	protected SmesMessageSpecification reset() {

		// configuration params - should they be reset?
		maxLengthSmsMessages = 140;
		timeFormatter = new AbsoluteTimeFormatter();

		sourceAddress = null;
		destinationAddress = null;
		serviceType = "CMT";
		sourceAddressTypeOfNumber = TypeOfNumber.UNKNOWN;
		sourceAddressNumberingPlanIndicator = NumberingPlanIndicator.UNKNOWN;
		destinationAddressTypeOfNumber = TypeOfNumber.UNKNOWN;
		destinationAddressNumberingPlanIndicator = NumberingPlanIndicator.UNKNOWN;
		esmClass = new ESMClass();
		protocolId = 0;
		priorityFlag = 1;
		scheduleDeliveryTime = null;
		validityPeriod = null;
		registeredDelivery = new RegisteredDelivery(SMSCDeliveryReceipt.DEFAULT);
		replaceIfPresentFlag = 0;
		dataCoding = new GeneralDataCoding(false, true, MessageClass.CLASS1, Alphabet.ALPHA_DEFAULT);
		smDefaultMsgId = 0;
		shortMessage = null; // the bytes to the 140 character text message
		smppSession = null;
		return this;
	}

	public SmesMessageSpecification setSourceAddressIfRequired(String defaultSourceAddress) {
		if (!StringUtils.hasText(this.sourceAddress))
			this.sourceAddress = defaultSourceAddress;
		return this;
	}
}

/*	private static String fromPropertyToHeaderConstant(String n) {

		StringBuffer stringBuffer = new StringBuffer();

		for (char c : n.toCharArray()) {
			if (Character.isUpperCase(c)) {
				stringBuffer.append("_");
			}
			stringBuffer.append(c);
		}

		String nn = stringBuffer.toString().toUpperCase();

		String is = "IS_",
				get = "GET_";
		if (nn.startsWith(is)) nn = nn.substring(is.length());
		if (nn.startsWith(get)) nn = nn.substring(get.length());

		return nn;
	}

	static public void main(String[] args) throws Throwable {
		String m = "mb.setHeader( SmppConstants.%s,  dsm.%s() );";
		String h = "public static final String %s = \"%s\";";
		Set<String> marshalling = new HashSet<String>();
		Set<String> headers = new HashSet<String>();
		PropertyDescriptor[] pds = PropertyUtils.getPropertyDescriptors(DeliverSm.class);
		for (PropertyDescriptor propertyDescriptor : pds) {
			Method reader = propertyDescriptor.getReadMethod();
			String readerName = reader.getName();

			String header = fromPropertyToHeaderConstant(readerName);
			headers.add(header);
			marshalling.add(readerName + ":" + header);
		}

		for (String s : headers) System.outSession.println(String.format(h, s, s));

		for (String s : marshalling) {

			String[] tuple = s.split(":");

			System.outSession.println(String.format(m, tuple[1], tuple[0]));
		}
	}
*/
