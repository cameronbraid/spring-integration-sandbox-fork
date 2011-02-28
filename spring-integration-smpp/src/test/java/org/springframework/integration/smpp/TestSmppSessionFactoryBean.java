package org.springframework.integration.smpp;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jsmpp.bean.*;
import org.jsmpp.extra.ProcessRequestException;
import org.jsmpp.session.DataSmResult;
import org.jsmpp.session.MessageReceiverListener;
import org.jsmpp.session.SMPPSession;
import org.jsmpp.session.Session;
import org.jsmpp.util.AbsoluteTimeFormatter;
import org.jsmpp.util.InvalidDeliveryReceiptException;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import java.util.Date;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Simple test, more of the SMPP API than anything, at the moment.
 *
 * Demonstrates that the {@link SmppSessionFactoryBean} works, too.
 *
 * @since 5.3
 * @author Josh Long
 */
public class TestSmppSessionFactoryBean {

	private Log logger = LogFactory.getLog(getClass());

	private SMPPSession smppSession;

	private AbsoluteTimeFormatter timeFormatter = new AbsoluteTimeFormatter();

	private String host = "127.0.0.1";

	private int port = 2775;

	private String systemId = "smppclient1";

	private String password = "password";

	private String smsMessageToSend ="jSMPP is truly a convenient, and powerful API for SMPP on " +
			"the Java and Spring Integration platforms (sent " +System.currentTimeMillis()+ ")";

	private AtomicInteger atomicInteger = new AtomicInteger();

	private final CyclicBarrier barrier = new CyclicBarrier(2);

	private SMPPSession buildSmppSession() throws Throwable {
		SmppSessionFactoryBean smppSessionFactoryBean = new SmppSessionFactoryBean();
		smppSessionFactoryBean.setHost(this.host);
		smppSessionFactoryBean.setPassword(this.password);
		smppSessionFactoryBean.setSystemId(this.systemId);
		smppSessionFactoryBean.setPort(this.port);
		smppSessionFactoryBean.setMessageReceiverListener(new CountingMessageReceiver());
		smppSessionFactoryBean.afterPropertiesSet();

		return smppSessionFactoryBean.getObject();
	}

	class CountingMessageReceiver implements MessageReceiverListener {

		public void onAcceptDeliverSm(DeliverSm deliverSm) throws ProcessRequestException {
			try {
				if (MessageType.SMSC_DEL_RECEIPT.containedIn(deliverSm.getEsmClass())) {
					// this message is delivery receipt which tells us that a <em>PREVIOUS</em> message that <em>we</em> originated has succeeded.
					try {
						DeliveryReceipt delReceipt = deliverSm.getShortMessageAsDeliveryReceipt();

						// lets cover the id to hex string format
						long id = Long.parseLong(delReceipt.getId());
						String messageId = Long.toString(id, 16).toUpperCase();
						logger.info("Receiving delivery receipt for message '" + messageId + " ' from "
								+ deliverSm.getSourceAddr() + " to " + deliverSm.getDestAddress() + " : " + delReceipt);
					} catch (InvalidDeliveryReceiptException e) {
						logger.warn("Failed in receiving delivery receipt", e);
					}
				} else { // this message is a regular short message
					String receivedMsg = new String(deliverSm.getShortMessage());
					Assert.assertEquals( "the sent SMS should be the same as the SMS we receive.", receivedMsg , smsMessageToSend);
					logger.info("Received message: " +  receivedMsg );
					atomicInteger.incrementAndGet();
					barrier.await();
					logger.info("just called barrier.await to trigger to the test method that the async message receipt " +
							"event has occured and that it can now check the counter()");
				}
			} catch (Throwable th) {
				logger.warn("something went wrong when trying to signal that a message had arrived.", th);
			}
		}

		public DataSmResult onAcceptDataSm(DataSm dataSm, Session source) throws ProcessRequestException {
			return null;
		}
		public void onAcceptAlertNotification(AlertNotification alertNotification) {
		}

	}

	@Before
	public void before() throws Throwable {
		if (logger.isInfoEnabled())
			logger.info("make sure you are running a working SMPP gateway. Seleniumsoftware has a simulator that is " +
					"available for free. See the readme.txt on testable implementations");

		SMPPSession s = buildSmppSession();
		((InitializingBean) s).afterPropertiesSet();
		this.smppSession = s;
		Assert.assertNotNull("the smppSession should not be null.", this.smppSession);
		logger.debug("created smppSession.");
	}

	@Test
	public void testSendingAndReceivingASmppMessage() throws Throwable {

		String messageId = this.smppSession.submitShortMessage(
				"CMT", TypeOfNumber.INTERNATIONAL,
				NumberingPlanIndicator.UNKNOWN,
				"1616", TypeOfNumber.INTERNATIONAL, NumberingPlanIndicator.UNKNOWN, "628176504657",
				new ESMClass(), (byte) 0, (byte) 1, timeFormatter.format(new Date()),
				null, new RegisteredDelivery(SMSCDeliveryReceipt.DEFAULT), (byte) 0, new GeneralDataCoding(false, true, MessageClass.CLASS1, Alphabet.ALPHA_DEFAULT),
				(byte) 0, smsMessageToSend.getBytes());

		Assert.assertNotNull("messageId should not be null", messageId);
		Assert.assertTrue("the returned message ID should not be -1", Integer.parseInt(messageId) >= 0);

		logger.debug("the returned messageId for the request was " + messageId);
		barrier.await();
		logger.debug("the counter says: " + this.atomicInteger.intValue());
		Assert.assertEquals("the counter should be equal to 1, to account for the one message we've sent.", 1, this.atomicInteger.intValue());
	}

	@After
	public void after() throws Throwable {
		((DisposableBean) smppSession).destroy();
	}
}
