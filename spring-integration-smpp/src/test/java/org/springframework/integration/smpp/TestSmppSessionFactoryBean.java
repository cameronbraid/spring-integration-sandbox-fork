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

import java.util.Date;

public class TestSmppSessionFactoryBean {


	private Log logger = LogFactory.getLog(getClass());

	private SMPPSession smppSession ;

	private String host = "127.0.0.1";
	private int port =   2775;
	private String systemId = "smppclient1";
	private String password="password";


	@Before
	public 	void before() throws Throwable {

		logger.debug( "make sure you are running a working SMPP gateway. Seleniumsoftware has one that's " +
				"available for free. See the readme.txt on testable implementations");


		SmppSessionFactoryBean smppSessionFactoryBean =
				 new SmppSessionFactoryBean();
		smppSessionFactoryBean.setHost(this.host);
		smppSessionFactoryBean.setPassword(this.password);
		smppSessionFactoryBean.setSystemId(this.systemId);
		smppSessionFactoryBean.setPort(this.port);
		smppSessionFactoryBean.afterPropertiesSet();

		this.smppSession = smppSessionFactoryBean.getObject();
		Assert.assertNotNull("the smppSession should not be null." ,this.smppSession );
		logger.debug( "created smppSession." );
	}

	@Test
	public void testSendingSmppMessage()  throws Throwable {
		AbsoluteTimeFormatter timeFormatter = new AbsoluteTimeFormatter();
		  String messageId = this.smppSession.submitShortMessage(
					"CMT", TypeOfNumber.INTERNATIONAL,
					NumberingPlanIndicator.UNKNOWN,
					"1616", TypeOfNumber.INTERNATIONAL, NumberingPlanIndicator.UNKNOWN, "628176504657",
					new ESMClass(), (byte)0, (byte)1,  timeFormatter.format(new Date()),
					null, new RegisteredDelivery(SMSCDeliveryReceipt.DEFAULT), (byte)0, new GeneralDataCoding(false, true, MessageClass.CLASS1, Alphabet.ALPHA_DEFAULT), (byte)0, "jSMPP simplify SMPP on Java platform".getBytes());

		Assert.assertNotNull("messageId should not be null", messageId) ;
		Assert.assertTrue("the returned message ID should not be -1", Integer.parseInt( messageId) >= 0);

		logger.debug( "the returned messageId for the request was " + messageId);
	}

	@Test public void testReceiving    (){
		// todo
	}

	@After
	public void after() throws Throwable {
		this.smppSession.unbindAndClose();
	}
}
