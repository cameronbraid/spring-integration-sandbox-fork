package org.springframework.integration.smpp;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jsmpp.bean.*;
import org.jsmpp.extra.ProcessRequestException;
import org.jsmpp.session.DataSmResult;
import org.jsmpp.session.MessageReceiverListener;
import org.jsmpp.session.Session;
import org.jsmpp.util.InvalidDeliveryReceiptException;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.integration.Message;
import org.springframework.integration.MessageChannel;
import org.springframework.integration.smpp.session.ExtendedSmppSession;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.Date;

/**
 * exercises the outbound adapter.
 *
 * @author Josh Long
 * @since 2.1
 */
//        @Ignore
@ContextConfiguration("classpath:TestSmppOutboundChannelAdapter-context.xml")
@RunWith(SpringJUnit4ClassRunner.class)
public class TestSmppOutboundChannelAdapter {

	private Log log = LogFactory.getLog(getClass());

	@Value("#{session}") private ExtendedSmppSession smppSession;

	@Value("#{outboundSms}") private MessageChannel messageChannel;

	private String smsMessageToSend = "jSMPP is truly a convenient, and powerful API for SMPP " +
			"on the Java and Spring Integration platforms (sent " + System.currentTimeMillis() + ")";

	class ConfirmingMessageReceiverListener implements MessageReceiverListener {
		public void onAcceptDeliverSm(DeliverSm deliverSm) throws ProcessRequestException {

			// this is a notification of the success or failure of a previous message
			if( MessageType.SMSC_DEL_RECEIPT.containedIn( deliverSm.getEsmClass())){
				try {
					DeliveryReceipt receipt = deliverSm.getShortMessageAsDeliveryReceipt();

					log.debug("the receipt is "+receipt.getFinalStatus().toString());
				} catch (InvalidDeliveryReceiptException e) {
				  throw new RuntimeException(e);
				}
			}
		}

		public void onAcceptAlertNotification(AlertNotification alertNotification) {
		}

		public DataSmResult onAcceptDataSm(DataSm dataSm, Session source) throws ProcessRequestException {
			return null;
		}
	}


	@Test
	public void testSendingAndReceivingASmppMessageUsingRawApi() throws Throwable {
		smppSession.addMessageReceiverListener(   new ConfirmingMessageReceiverListener());
		Message<String> smsMsg = MessageBuilder.withPayload(this.smsMessageToSend)
				.setHeader(SmppConstants.SRC_ADDR, "1616")
				.setHeader(SmppConstants.DST_ADDR, "628176504657")
				.setHeader(SmppConstants.REGISTERED_DELIVERY_MODE, SMSCDeliveryReceipt.SUCCESS_FAILURE)
				.setHeader(SmppConstants.SCHEDULED_DELIVERY_TIME, new Date()).build();

		this.messageChannel.send(smsMsg);

		Thread.sleep(1000);

/*
				SMPPSession smppSession  =null;smppSession.submitShortMessage(
				"CMT", TypeOfNumber.INTERNATIONAL,
				NumberingPlanIndicator.UNKNOWN,
				"1616", TypeOfNumber.INTERNATIONAL, NumberingPlanIndicator.UNKNOWN, "628176504657",
				new ESMClass(), (byte) 0, (byte) 1, timeFormatter.format(new Date()),
				null, new RegisteredDelivery(SMSCDeliveryReceipt.DEFAULT), (byte) 0, new GeneralDataCoding(false, true, MessageClass.CLASS1, Alphabet.ALPHA_DEFAULT),
				(byte) 0, smsMessageToSend.getBytes());*/
/*
		String messageId = this.smppSession.submitShortMessage(
				"CMT", TypeOfNumber.INTERNATIONAL,
				NumberingPlanIndicator.UNKNOWN,
				"1616", TypeOfNumber.INTERNATIONAL, NumberingPlanIndicator.UNKNOWN, "628176504657",
				new ESMClass(), (byte) 0, (byte) 1, timeFormatter.format(new Date()),
				null, new RegisteredDelivery(SMSCDeliveryReceipt.DEFAULT), (byte) 0, new GeneralDataCoding(false, true, MessageClass.CLASS1, Alphabet.ALPHA_DEFAULT),
				(byte) 0, smsMessageToSend.getBytes());*/

/*		Assert.assertNotNull("messageId should not be null", messageId);
		Assert.assertTrue("the returned message ID should not be -1", Integer.parseInt(messageId) >= 0);

		logger.debug("the returned messageId for the request was " + messageId);*/
	}
}
