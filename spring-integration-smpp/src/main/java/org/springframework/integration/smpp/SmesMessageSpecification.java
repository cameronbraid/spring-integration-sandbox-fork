package org.springframework.integration.smpp;

import org.jsmpp.bean.*;
import org.jsmpp.session.SMPPSession;
import org.jsmpp.util.AbsoluteTimeFormatter;
import org.jsmpp.util.TimeFormatter;
import org.springframework.aop.framework.ProxyFactoryBean;
import org.springframework.core.NamedThreadLocal;
import org.springframework.integration.Message;
import org.springframework.integration.smpp.util.CurrentExecutingMethodHolder;
import org.springframework.integration.smpp.util.CurrentMethodExposingMethodInterceptor;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
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

	// pool of SmesMessageSpecification objects
	private static final ThreadLocal<SmesMessageSpecification> poolOfSpecifications =
			new NamedThreadLocal<SmesMessageSpecification>(SmesMessageSpecification.class.getName().toLowerCase());

	private TimeFormatter timeFormatter = new AbsoluteTimeFormatter();

	private String sourceAddr;
	private String destinationAddr;
	private String serviceType;
	private TypeOfNumber sourceAddrTon;
	private NumberingPlanIndicator sourceAddrNpi;
	private TypeOfNumber destAddrTon;
	private NumberingPlanIndicator destAddrNpi;
	private ESMClass esmClass;
	private byte protocolId;
	private byte priorityFlag;
	private String scheduleDeliveryTime;
	private String validityPeriod;
	private RegisteredDelivery registeredDelivery;
	private byte replaceIfPresentFlag;
	private DataCoding dataCoding;
	private byte smDefaultMsgId;
	private byte[] shortMessage;
	private SMPPSession smppSession;

	/**
	 * this method will take an inbound Spring Integration {@link Message} and map it to a {@link SmesMessageSpecification}
	 * which we can use to send the SMS message.
	 *
	 * @param msg a new {@link Message}
	 * @return a {@link SmesMessageSpecification}
	 */
	public static SmesMessageSpecification fromMessage(SMPPSession smppSession, Message<?> msg) {

		String srcAddy = valueIfHeaderExists(SRC_ADDR, msg);
		String dstAddy = valueIfHeaderExists(DST_ADDR, msg);
		String smsTxt = valueIfHeaderExists(SMS_MSG, msg);
		if (!StringUtils.hasText(smsTxt)) {
			Object payload = msg.getPayload();
			if (payload instanceof String) {
				smsTxt = (String) payload;
			}
		}

		SmesMessageSpecification smesMessageSpecification = SmesMessageSpecification.newSMESMessageSpecification(smppSession, srcAddy, dstAddy, smsTxt)
				.setDestinationAddressNumberingPlanIndicator(SmesMessageSpecification.<NumberingPlanIndicator>valueIfHeaderExists(DST_NPI, msg))
				.setSourceAddressNumberingPlanIndicator(SmesMessageSpecification.<NumberingPlanIndicator>valueIfHeaderExists(SRC_NPI, msg))
				.setDestinationAddressTypeOfNumber(SmesMessageSpecification.<TypeOfNumber>valueIfHeaderExists(DST_TON, msg))
				.setSourceAddressTypeOfNumber(SmesMessageSpecification.<TypeOfNumber>valueIfHeaderExists(SRC_TON, msg))
				.setServiceType(SmesMessageSpecification.<String>valueIfHeaderExists(SERVICE_TYPE, msg))
				.setEsmClass(SmesMessageSpecification.<ESMClass>valueIfHeaderExists(ESM_CLASS, msg))
				.setProtocolId(SmesMessageSpecification.<Byte>valueIfHeaderExists(PROTOCOL_ID, msg))
				.setScheduleDeliveryTime(SmesMessageSpecification.<Date>valueIfHeaderExists(SCHEDULED_DELIVERY_TIME, msg))
				.setValidityPeriod(SmesMessageSpecification.<String>valueIfHeaderExists(VALIDITY_PERIOD, msg))
				.setPriorityFlag(SmesMessageSpecification.<Byte>valueIfHeaderExists(PRIORITY_FLAG, msg))
				.setDataCoding(SmesMessageSpecification.<DataCoding>valueIfHeaderExists(DATA_CODING, msg))
				.setSmDefaultMsgId(SmesMessageSpecification.<Byte>valueIfHeaderExists(SM_DEFAULT_MSG_ID, msg))
				.setReplaceIfPresentFlag(SmesMessageSpecification.<Byte>valueIfHeaderExists(REPLACE_IF_PRESENT_FLAG, msg))
				.setRegisteredDelivery(registeredDeliveryFromHeader(msg));

		return smesMessageSpecification;
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

		if (rd instanceof RegisteredDelivery) {
			return (RegisteredDelivery) rd;
		}
		return null;
	}

	/**
	 * you need to use the builder API
	 */
	SmesMessageSpecification(SMPPSession smppSession) {
		this.smppSession = smppSession;
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
	SmesMessageSpecification setSmppSession(SMPPSession ss) {
		this.smppSession = ss;
		return this;
	}

	/**
	 * Resets the thread local, pooled objects to a known state before reuse.
	 * <p/>
	 * Resetting the variables is trivially cheap compared to proxying a new one each time.
	 *
	 * @return the cleaned up {@link SmesMessageSpecification}
	 */
	public SmesMessageSpecification reset() {
		sourceAddr = null;
		destinationAddr = null;
		serviceType = "CMT";
		sourceAddrTon = TypeOfNumber.UNKNOWN;
		sourceAddrNpi = NumberingPlanIndicator.UNKNOWN;
		destAddrTon = TypeOfNumber.UNKNOWN;
		destAddrNpi = NumberingPlanIndicator.UNKNOWN;
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

	/**
	 * use the builder API, but we need this to cleanly proxy
	 */
	SmesMessageSpecification() {
	}

	/**
	 * Conceptually, you could get away with just specifying these three parameters, though I don't know how likely that is in practice.
	 *
	 * @param srcAddress	the source address
	 * @param destAddress the destination address
	 * @param txtMessage	the message to send (must be  no more than 140 characters
	 * @return the {@link SmesMessageSpecification}
	 */
	public static SmesMessageSpecification newSMESMessageSpecification(SMPPSession ss, String srcAddress, String destAddress, String txtMessage) {

		SmesMessageSpecification smesMessageSpecification = poolOfSpecifications.get();

		if (null == smesMessageSpecification) {
			ProxyFactoryBean proxyFactoryBean = new ProxyFactoryBean();
			proxyFactoryBean.setProxyTargetClass(true);
			proxyFactoryBean.setBeanClassLoader(ClassUtils.getDefaultClassLoader());
			proxyFactoryBean.setTarget(new SmesMessageSpecification());
			proxyFactoryBean.addAdvice(new CurrentMethodExposingMethodInterceptor());
			smesMessageSpecification = (SmesMessageSpecification) proxyFactoryBean.getObject();
			poolOfSpecifications.set(smesMessageSpecification);
		}

		smesMessageSpecification
				.reset()
				.setSmppSession(ss)
				.setSourceAddress(srcAddress)
				.setDestinationAddress(destAddress)
				.setShortTextMessage(txtMessage);

		return smesMessageSpecification;
	}

	/**
	 * send the message on its way.
	 * <p/>
	 * todo can we do something smart here or through an adapter to handle the situation where we have asked for a message receipt? what about if we're using a message receipt <em>and</eM> we're only a receiver or a sender connection and not a transceiver? We need gateway semantics across two unidirectional SMPPSessions, then
	 *
	 * @throws Exception the {@link SMPPSession#submitShortMessage(String, org.jsmpp.bean.TypeOfNumber, org.jsmpp.bean.NumberingPlanIndicator, String, org.jsmpp.bean.TypeOfNumber, org.jsmpp.bean.NumberingPlanIndicator, String, org.jsmpp.bean.ESMClass, byte, byte, String, String, org.jsmpp.bean.RegisteredDelivery, byte, org.jsmpp.bean.DataCoding, byte, byte[], org.jsmpp.bean.OptionalParameter...)} method throws lots of Exceptions, including {@link java.io.IOException}
	 */
	public String send() throws Exception {
		validate();
		String messageId = this.smppSession.submitShortMessage(this.serviceType, this.sourceAddrTon, this.sourceAddrNpi, this.sourceAddr, this.destAddrTon, destAddrNpi, this.destinationAddr, this.esmClass, this.protocolId, this.priorityFlag, this.scheduleDeliveryTime, this.validityPeriod, this.registeredDelivery, this.replaceIfPresentFlag, this.dataCoding, this.smDefaultMsgId, this.shortMessage);
		return messageId;
	}

	protected void validate() {
		Assert.notNull(this.sourceAddr, "the source address must not be null");
		Assert.notNull(this.destinationAddr, "the destination address must not be null");
		Assert.isTrue(this.shortMessage != null && this.shortMessage.length > 0, "the message must not be null");
	}

	public SmesMessageSpecification setSourceAddress(String sourceAddr) {
		this.sourceAddr = sourceAddr;
		return this;
	}

	public SmesMessageSpecification setDestinationAddress(String destinationAddr) {
		this.destinationAddr = destinationAddr;
		return this;
	}

	public SmesMessageSpecification setServiceType(String serviceType) {
		this.serviceType = serviceType;
		return this;
	}

	public SmesMessageSpecification setSourceAddressTypeOfNumber(TypeOfNumber sourceAddrTon) {
		this.sourceAddrTon = sourceAddrTon;
		return this;
	}

	public SmesMessageSpecification setSourceAddressNumberingPlanIndicator(NumberingPlanIndicator sourceAddrNpi) {
		this.sourceAddrNpi = sourceAddrNpi;
		return this;
	}

	public SmesMessageSpecification setDestinationAddressTypeOfNumber(TypeOfNumber destAddrTon) {
		this.destAddrTon = destAddrTon;
		return this;
	}

	/**
	 * guard against overwriting perfectly good defaults with null values.
	 *
	 * @param v value the value
	 */
	private void errorOnNullHeaderSet(Object v) {
		String methodName = CurrentExecutingMethodHolder.getCurrentlyExecutingMethod().getName();
		if (StringUtils.hasText(methodName)) {
			if (methodName.toLowerCase().startsWith("set")) {
				methodName = methodName.substring(3);
				methodName = (methodName.charAt(0) + "").toLowerCase() + methodName.substring(1);
			}
			Assert.notNull(v, "the property ['" + methodName + "'] can't be null. There is a default in place, but don't overwrite it with null");
		} else {
			Assert.notNull(v, "There is a default in place for this property; don't overwrite it with null");
		}
	}

	public SmesMessageSpecification setDestinationAddressNumberingPlanIndicator(NumberingPlanIndicator destAddrNpi) {
		this.destAddrNpi = destAddrNpi;
		return this;
	}

	public SmesMessageSpecification setEsmClass(ESMClass esmClass) {
		errorOnNullHeaderSet(esmClass);
		this.esmClass = esmClass;
		return this;
	}

	public SmesMessageSpecification setProtocolId(byte protocolId) {
		errorOnNullHeaderSet(protocolId);
		this.protocolId = protocolId;
		return this;
	}

	public SmesMessageSpecification setPriorityFlag(byte pf) {
		errorOnNullHeaderSet(pf);
		this.priorityFlag = pf;
		return this;
	}

	/**
	 * When you submit a message to an SMSC, it is possible to sometimes specify a
	 * <em>validity period</em> for the message. This setting is an instruction to the SMSC that stipulates that
	 * if the message cannot be delivered to the recipient within the next N minutes or hours or days,
	 * the SMSC should discard the message. This would mean that if the recipient's mobile phone is
	 * turned off, or out of coverage for x minutes/hours/days after the message is submitted, the SMSC
	 * should not perform further delivery retry and should discard the message.
	 * <p/>
	 * Of course, there is no guarantee that the operator SMSC will respect this setting, so it needs
	 * to be tested with a particular operator first to determine if it can be used reliably.
	 * <p/>
	 * That information came from <a href="http://www.nowsms.com/smpp-information">the NowSMS website.</a>.
	 */
	public SmesMessageSpecification setValidityPeriod(String validityPeriod) {
		this.validityPeriod = validityPeriod;
		return this;
	}

	public SmesMessageSpecification setScheduleDeliveryTime(Date d) {
		errorOnNullHeaderSet(d);
		this.scheduleDeliveryTime = timeFormatter.format(d);
		return this;
	}

	public SmesMessageSpecification setRegisteredDelivery(RegisteredDelivery registeredDelivery) {
		errorOnNullHeaderSet(registeredDelivery);
		this.registeredDelivery = registeredDelivery;
		return this;
	}

	public SmesMessageSpecification setReplaceIfPresentFlag(byte replaceIfPresentFlag) {
		errorOnNullHeaderSet(replaceIfPresentFlag);
		this.replaceIfPresentFlag = replaceIfPresentFlag;
		return this;
	}

	public SmesMessageSpecification setDataCoding(DataCoding dataCoding) {
		errorOnNullHeaderSet(dataCoding);
		this.dataCoding = dataCoding;
		return this;
	}

	public SmesMessageSpecification setSmDefaultMsgId(byte smDefaultMsgId) {
		this.smDefaultMsgId = smDefaultMsgId;
		return this;
	}

	public SmesMessageSpecification setTimeFormatter(TimeFormatter timeFormatter) {
		errorOnNullHeaderSet(timeFormatter);
		this.timeFormatter = timeFormatter;
		return this;
	}

	/**
	 * todo it's not <em>quite</em> true that the payload needs to be 140c. The message can be split up into smaller messages,
	 * but for now it's more useful to have this validation in place than not.
	 *
	 * @param s the text message body
	 * @return the SmesMessageSpecification
	 */
	public SmesMessageSpecification setShortTextMessage(String s) {
		Assert.notNull(s, "the SMS message payload must not be null");
		Assert.isTrue(s.length() <= 140, "the SMS message payload must be 140 characters or less.");
		this.shortMessage = s.getBytes();
		return this;
	}
}
