package org.springframework.integration.smpp;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jsmpp.bean.*;
import org.jsmpp.extra.ProcessRequestException;
import org.jsmpp.session.DataSmResult;
import org.jsmpp.session.MessageReceiverListener;
import org.jsmpp.session.Session;
import org.jsmpp.util.InvalidDeliveryReceiptException;
import org.junit.Assert;

import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Simple class to help verify that X messages are sent and X messages are received.
 *
 * @author Josh Long
 * @since 2.1
 */
public class CountingMessageReceiver implements MessageReceiverListener {

	private Log logger = LogFactory.getLog(getClass());

	private AtomicInteger atomicInteger;
	private CyclicBarrier barrier = null;
	private String smsMessageToSend;

	public CountingMessageReceiver(String sms, CyclicBarrier cb, AtomicInteger atomicInteger) {
		this.atomicInteger = atomicInteger;
		this.barrier = cb;
		this.smsMessageToSend = sms;
	}

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
				Assert.assertEquals("the sent SMS should be the same as the SMS we receive.", receivedMsg, smsMessageToSend);
				logger.info("Received message: " + receivedMsg);
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
		return null;		 // noop
	}

	public void onAcceptAlertNotification(AlertNotification alertNotification) {
		// noop
	}
}
