package org.springframework.integration.smpp;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jsmpp.bean.AlertNotification;
import org.jsmpp.bean.DataSm;
import org.jsmpp.bean.DeliverSm;
import org.jsmpp.extra.ProcessRequestException;
import org.jsmpp.session.*;
import org.jsmpp.util.AbsoluteTimeFormatter;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.integration.smpp.session.DelegatingMessageReceiverListener;
import org.springframework.integration.smpp.session.ExtendedSmppSession;
import org.springframework.integration.smpp.session.ExtendedSmppSessionAdaptingDelegate;
import org.springframework.integration.smpp.session.SmppSessionFactoryBean;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Field;
import java.util.Set;

/**
 * Simple test, more of the SMPP API than anything, at the moment.
 * <p/>
 * Demonstrates that the {@link org.springframework.integration.smpp.session.SmppSessionFactoryBean} works, too.
 *
 * @author Josh Long
 * @since 5.3
 */
public class TestSmppSessionFactoryBean {

	private Log logger = LogFactory.getLog(getClass());

	private ClientSession smppSession;

	private AbsoluteTimeFormatter timeFormatter = new AbsoluteTimeFormatter();

	private String host = "127.0.0.1";

	private int port = 2775;

	private String systemId = "smppclient1";

	private String password = "password";

	private String smsMessageToSend = "jSMPP is truly a convenient, and powerful API for SMPP on " +
			"the Java and Spring Integration platforms (sent " + System.currentTimeMillis() + ")";

	@Test
	public void testSmppSessionFactory() throws Throwable {

		SmppSessionFactoryBean smppSessionFactoryBean =
				new SmppSessionFactoryBean();
		smppSessionFactoryBean.setSystemId(this.systemId);
		smppSessionFactoryBean.setPort(this.port);
		smppSessionFactoryBean.setPassword(this.password);
		smppSessionFactoryBean.setHost(this.host);
		smppSessionFactoryBean.afterPropertiesSet();
		ExtendedSmppSession extendedSmppSession = smppSessionFactoryBean.getObject();
		Assert.assertNotNull("the factoried object should not be null", extendedSmppSession);
		extendedSmppSession.addMessageReceiverListener(new MessageReceiverListener() {
			public void onAcceptDeliverSm(DeliverSm deliverSm) throws ProcessRequestException {

			}
			public void onAcceptAlertNotification(AlertNotification alertNotification) {
			}
			public DataSmResult onAcceptDataSm(DataSm dataSm, Session source) throws ProcessRequestException {
				return null;
			}
		});
		Assert.assertEquals(extendedSmppSession.getClass(), ExtendedSmppSessionAdaptingDelegate.class);
		ExtendedSmppSessionAdaptingDelegate extendedSmppSessionAdaptingDelegate = (ExtendedSmppSessionAdaptingDelegate) extendedSmppSession;
		Assert.assertNotNull(extendedSmppSessionAdaptingDelegate.getTargetClientSession());
		Assert.assertTrue(extendedSmppSessionAdaptingDelegate.getTargetClientSession() instanceof SMPPSession);
		final SMPPSession s = (SMPPSession) extendedSmppSessionAdaptingDelegate.getTargetClientSession();

		ReflectionUtils.doWithFields(SMPPSession.class, new ReflectionUtils.FieldCallback() {
			public void doWith(Field field) throws IllegalArgumentException, IllegalAccessException {
				if (field.getName().equalsIgnoreCase("messageReceiverListener")) {
					field.setAccessible(true);
					MessageReceiverListener messageReceiverListener = (MessageReceiverListener) field.get(s);
					Assert.assertNotNull(messageReceiverListener);
					Assert.assertTrue(messageReceiverListener instanceof DelegatingMessageReceiverListener);
					final DelegatingMessageReceiverListener delegatingMessageReceiverListener = (DelegatingMessageReceiverListener) messageReceiverListener;
					ReflectionUtils.doWithFields(DelegatingMessageReceiverListener.class, new ReflectionUtils.FieldCallback() {
						public void doWith(Field field) throws IllegalArgumentException, IllegalAccessException {
							if (field.getName().equals("messageReceiverListenerSet")) {
								field.setAccessible(true);
								Set<MessageReceiverListener> l = (Set<MessageReceiverListener>) field.get(delegatingMessageReceiverListener);
								Assert.assertEquals(l.size(), 1);
							}
						}
					});
				}
			}
		});
	}
}
