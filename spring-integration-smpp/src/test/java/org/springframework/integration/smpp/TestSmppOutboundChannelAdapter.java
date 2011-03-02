package org.springframework.integration.smpp;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jsmpp.bean.SMSCDeliveryReceipt;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.integration.Message;
import org.springframework.integration.MessageChannel;
import org.springframework.integration.smpp.session.ExtendedSmppSession;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.concurrent.CyclicBarrier;

/**
 * exercises the outbound adapter.
 *
 * @author Josh Long
 * @since 2.1
 */
@ContextConfiguration("classpath:TestSmppOutboundChannelAdapter-context.xml")
@RunWith(SpringJUnit4ClassRunner.class)
public class TestSmppOutboundChannelAdapter {


	private Log log = LogFactory.getLog(getClass());

	@Value("#{session}")
	private ExtendedSmppSession smppSession;

	@Value("#{outboundSms}")
	private MessageChannel messageChannel;

	private String smsMessageToSend = "jSMPP is truly a convenient, and powerful API for SMPP " +
			"on the Java and Spring Integration platforms (sent " + System.currentTimeMillis() + ")";

	@Test
	public void testSendingAndReceivingASmppMessageUsingRawApi() throws Throwable {

		Message<String> smsMsg = MessageBuilder.withPayload(this.smsMessageToSend)
				.setHeader(SmppConstants.SRC_ADDR, "1616")
				.setHeader(SmppConstants.DST_ADDR, "628176504657")
				.setHeader(SmppConstants.REGISTERED_DELIVERY_MODE, SMSCDeliveryReceipt.SUCCESS)
				.build();

		this.messageChannel.send(smsMsg);
	}
}
