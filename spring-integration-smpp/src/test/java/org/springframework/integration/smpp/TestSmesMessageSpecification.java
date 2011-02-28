package org.springframework.integration.smpp;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jsmpp.bean.*;
import org.jsmpp.session.SMPPSession;
import org.jsmpp.util.AbsoluteTimeFormatter;
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
 * @author Josh Long
 */
public class TestSmesMessageSpecification {

	private Log logger = LogFactory.getLog(getClass());

	private SMPPSession smppSession;

	private AbsoluteTimeFormatter timeFormatter = new AbsoluteTimeFormatter();

	private String host = "127.0.0.1";

	private int port = 2775;

	private String systemId = "smppclient1";

	private String password = "password";

	private String smsMessageToSend = "jSMPP is truly a convenient, and powerful API for SMPP on " +
			"the Java and Spring Integration platforms (sent " + System.currentTimeMillis() + ")";

	private AtomicInteger atomicInteger = new AtomicInteger();

	private final CyclicBarrier barrier = new CyclicBarrier(2);

	private SMPPSession buildSmppSession() throws Throwable {
		SmppSessionFactoryBean smppSessionFactoryBean = new SmppSessionFactoryBean();
		smppSessionFactoryBean.setHost(this.host);
		smppSessionFactoryBean.setPassword(this.password);
		smppSessionFactoryBean.setSystemId(this.systemId);
		smppSessionFactoryBean.setPort(this.port);
		smppSessionFactoryBean.setMessageReceiverListener(new CountingMessageReceiver(this.smsMessageToSend, barrier, atomicInteger));
		smppSessionFactoryBean.afterPropertiesSet();

		return smppSessionFactoryBean.getObject();
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
	public void testSendingAndReceivingASmppMessageUsingRawApi() throws Throwable {

		String messageId  = SmesMessageSpecification.newSMESMessageSpecification(
				smppSession,"1616", "628176504657",smsMessageToSend).send();
		Assert.assertNotNull("messageId should not be null", messageId);
		Assert.assertTrue("the returned message ID should not be -1", Integer.parseInt(messageId) >= 0);

		logger.debug("the returned messageId for the request was " + messageId);
		barrier.await();
		logger.debug("the counter says: " + this.atomicInteger.intValue());
		Assert.assertEquals("the counter should be equal to 1, to account for the one message we've sent.", 1, this.atomicInteger.intValue());
	}

	@Test( expected = IllegalArgumentException.class)
	public void testFaultySmesMessageSpecification () throws Throwable {
			String messageId  = SmesMessageSpecification.newSMESMessageSpecification(
				smppSession,"1616", "628176504657",smsMessageToSend)
					.setEsmClass(null)   // this will cause an error
					.send();

	}

	@After
	public void after() throws Throwable {
		((DisposableBean) smppSession).destroy();
	}
}
