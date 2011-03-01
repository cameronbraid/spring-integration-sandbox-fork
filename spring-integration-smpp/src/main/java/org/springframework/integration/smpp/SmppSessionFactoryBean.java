package org.springframework.integration.smpp;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jsmpp.*;
import org.jsmpp.bean.BindType;
import org.jsmpp.bean.NumberingPlanIndicator;
import org.jsmpp.bean.TypeOfNumber;
import org.jsmpp.session.BindParameter;
import org.jsmpp.session.MessageReceiverListener;
import org.jsmpp.session.SMPPSession;
import org.jsmpp.session.SessionStateListener;
import org.jsmpp.session.connection.Connection;
import org.jsmpp.session.connection.ConnectionFactory;
import org.jsmpp.session.connection.socket.SocketConnection;
import org.jsmpp.util.DefaultComposer;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

import javax.net.SocketFactory;
import javax.net.ssl.SSLSocketFactory;
import java.io.IOException;
import java.net.Socket;

/**
 * Factory bean to create a {@link SMPPSession}. Usually, you need little more than the {@link #host},
 * the {@link #port}, perhaps a {@link #password}, and a {@link #systemId}.
 * <p/>
 * The {@link SMPPSession } represents a connection to a SMSC, through which SMS messages are sent and received.
 * <p/>
 * Here is a breakdown of the supported parameters on this factory bean:
 * <p/>
 * host				 the SMSC host to which the session is bound   (think of this as the host of your email server)
 * port				 the SMSC port to which the session is bound (think of this as a port on your email server)
 * bindType		 values of type {@link org.jsmpp.bean.BindType}. the bind type specifies whether this {@link SMPPSession} can send ({@link org.jsmpp.bean.BindType#BIND_TX}), receive ({@link org.jsmpp.bean.BindType#BIND_RX}), or both send and receive ({@link org.jsmpp.bean.BindType#BIND_TRX}).
 * systemId		 the system ID for the server being bound to
 * password		 the password for the server being bound to
 * systemType	 the SMSC system type
 * addrTon			a value from the {@link org.jsmpp.bean.TypeOfNumber} enumeration. default is {@link org.jsmpp.bean.TypeOfNumber#UNKNOWN}
 * addrNpi			a value from  the {@link org.jsmpp.bean.NumberingPlanIndicator} enumeration. Default is {@link org.jsmpp.bean.NumberingPlanIndicator#UNKNOWN}
 * addressRange can be null. Specifies the address range.
 * timeout			a good default value is 60000  (1 minute)
 *
 * @author Josh Long
 *         <p/>
 *         todo support a proxied SMPPSession that automatically recovers from disconnects a la the examples {@link org.jsmpp.examples.gateway.AutoReconnectGateway}
 * @see org.jsmpp.session.SMPPSession#SMPPSession()
 * @see org.jsmpp.session.SMPPSession#connectAndBind(String, int, org.jsmpp.session.BindParameter)
 * @see org.jsmpp.session.SMPPSession#connectAndBind(String, int, org.jsmpp.bean.BindType, String, String, String, org.jsmpp.bean.TypeOfNumber, org.jsmpp.bean.NumberingPlanIndicator, String, long)
 * @since 2.1
 */
public class SmppSessionFactoryBean implements FactoryBean<SMPPSession>, InitializingBean {

	private Log log = LogFactory.getLog(getClass());
	private SessionStateListener sessionStateListener;
	private boolean ssl = false;
	private String host = "127.0.0.1";
	private String addressRange;
	private long timeout = 60 * 1000;// 1 minute
	private int port = 2775;	// good default though this has been known to change
	private BindType bindType = BindType.BIND_TRX; // bind as a 'transceiver' - only 3.4 of the spec <em>requires</em> support for this
	private String systemId = getClass().getSimpleName().toLowerCase();	 // what would typically be called 'user' in a user/pw scheme
	private String password;
	private String systemType = "cp";
	private TypeOfNumber addrTon = TypeOfNumber.UNKNOWN;
	private NumberingPlanIndicator addrNpi = NumberingPlanIndicator.UNKNOWN;
	private MessageReceiverListener messageReceiverListener;

	public void setSsl(boolean ssl) {
		this.ssl = ssl;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public void setBindType(BindType bindType) {
		this.bindType = bindType;
	}

	public void setSystemId(String systemId) {
		this.systemId = systemId;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public void setSystemType(String systemType) {
		this.systemType = systemType;
	}

	public void setAddrTon(TypeOfNumber addrTon) {
		this.addrTon = addrTon;
	}

	public void setAddrNpi(NumberingPlanIndicator addrNpi) {
		this.addrNpi = addrNpi;
	}

	public void setAddressRange(String addressRange) {
		this.addressRange = addressRange;
	}

	public void setTimeout(long timeout) {
		this.timeout = timeout;
	}

	public void setMessageReceiverListener(MessageReceiverListener messageReceiverListener) {
		this.messageReceiverListener = messageReceiverListener;
	}

	public void setSessionStateListener(SessionStateListener sessionStateListener) {
		this.sessionStateListener = sessionStateListener;
	}

	/**
	 * hook to add extra objects <em>before</em> the {@link SMPPSession#connectAndBind(String, int, org.jsmpp.session.BindParameter)} invocation.
	 *
	 * @param smppSession the session to be customized
	 * @throws Exception if anything generally goes wrong
	 */
	private void customizeSmppSession(SMPPSession smppSession) throws Exception {

		if (this.messageReceiverListener != null)
			smppSession.setMessageReceiverListener(this.messageReceiverListener);

		if (this.sessionStateListener != null)
			smppSession.addSessionStateListener(this.sessionStateListener);
	}

	/**
	 * we return a subclass of {@link SMPPSession} - {@link InitializationAndDisposalAwareSmppSession a subclass that knows about the {@link InitializingBean} and {@link DisposableBean} interfaces} -
	 * to ensure that the SMPPSession is adapted for Spring's lifecycle managment hooks.
	 * Additionally, we use this method to provide a bit of indirection so that we can plugin an alternate {@link ConnectionFactory} implementation to support SSL.
	 *
	 * @return the configured SMPPSession
	 * @throws Exception should anything go wrong
	 */
	private SMPPSession buildSmppSession() throws Exception {
		SMPPSession smppSession = null;
		if (!ssl) {
			smppSession = new InitializationAndDisposalAwareSmppSession();
		} else {
			smppSession = new InitializationAndDisposalAwareSmppSession(new SynchronizedPDUSender(new DefaultPDUSender(new DefaultComposer())), new DefaultPDUReader(), sslConnectionFactory);
		}
		customizeSmppSession(smppSession);

		// todo shouldnt this automatically get called by the container on the bean once its returned <em>from</em> the factory?
		((InitializationAndDisposalAwareSmppSession) smppSession).afterPropertiesSet();

		return smppSession;
	}

	/**
	 * delegates to {@link #buildSmppSession()}
	 */
	@Override
	public SMPPSession getObject() throws Exception {
		return buildSmppSession();
	}

	/**
	 * Adapting {@link SMPPSession} subclass that makes the {@link SMPPSession} participate in Spring lifecycle hooks.
	 */
	@SuppressWarnings("unused")
	private class InitializationAndDisposalAwareSmppSession extends SMPPSession implements InitializingBean, DisposableBean {

		InitializationAndDisposalAwareSmppSession() {
		}

		InitializationAndDisposalAwareSmppSession(PDUSender pduSender, PDUReader pduReader, ConnectionFactory connFactory) {
			super(pduSender, pduReader, connFactory);
		}

		InitializationAndDisposalAwareSmppSession(String host, int port, BindParameter bindParam, PDUSender pduSender, PDUReader pduReader, ConnectionFactory connFactory) throws IOException {
			super(host, port, bindParam, pduSender, pduReader, connFactory);
		}

		InitializationAndDisposalAwareSmppSession(String host, int port, BindParameter bindParam) throws IOException {
			super(host, port, bindParam);
		}

		@Override
		public void destroy() throws Exception {
			log.debug(DisposableBean.class.getName() + "#destroy() called. calling closeSmppSessionSafely(this.smppSession).");
			closeSmppSessionSafely(this);
		}

		@Override
		public void afterPropertiesSet() throws Exception {
			log.debug(InitializingBean.class.getName() + "#afterPropertiesSet called. calling connectAndBindSmppSessionSafely(this.smppSession).");
			connectAndBindSmppSessionSafely(this);
		}
	}

	private void closeSmppSessionSafely(SMPPSession session) {
		if (session != null) {
			if (session.getSessionState().isBound()) {
				try {
					session.unbindAndClose();
				} catch (Throwable t) {
					log.warn("couldn't close and unbind the session", t);
				}
			}
		} else {
			log.warn("the smppSession given to close is null");
		}
	}

	private void connectAndBindSmppSessionSafely(SMPPSession smppSession) throws Exception {
		if (smppSession != null) {
			if (smppSession.getSessionState().isBound()) {
				log.warn("the session is already bound. Returning without any changes..");
				return;
			}
			smppSession.connectAndBind(host, port, bindType, systemId, password, systemType, addrTon, addrNpi, addressRange, timeout);
		}
	}

	@Override
	public Class<?> getObjectType() {
		return SMPPSession.class;
	}

	@Override
	public boolean isSingleton() {
		return false;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		Assert.notNull(this.systemId, "the systemId can't be null");
		Assert.notNull(this.host, "the host can't be null");
		Assert.notNull(this.port, "the port can't be null");
	}

	final private static ConnectionFactory sslConnectionFactory = new ConnectionFactory() {
		@Override
		public Connection createConnection(String host, int port) throws IOException {
			SocketFactory socketFactory = SSLSocketFactory.getDefault();
			Socket socket = socketFactory.createSocket(host, port);
			return new SocketConnection(socket);
		}
	};
}

/*    private void reconnectAfter(final long timeInMillis) {
        new Thread() {
            @Override
            public void run() {
                logger.info("Schedule reconnect after " + timeInMillis + " millis");
                try {
                    Thread.sleep(timeInMillis);
                } catch (InterruptedException e) {
                }

                int attempt = 0;
                while (session == null || session.getSessionState().equals(SessionState.CLOSED)) {
                    try {
                        logger.info("Reconnecting attempt #" + (++attempt) + "...");
                        session = newSession();
                    } catch (IOException e) {
                        logger.error("Failed opening connection and bind to " + remoteIpAddress + ":" + remotePort, e);
                        // wait for a second
                        try { Thread.sleep(1000); } catch (InterruptedException ee) {}
                    }
                }
            }
        }.start();
    }


     private class SessionStateListenerImpl implements SessionStateListener {
        public void onStateChange(SessionState newState, SessionState oldState,
                Object source) {
            if (newState.equals(SessionState.CLOSED)) {
                logger.info("Session closed");
                reconnectAfter(reconnectInterval);
            }
        }
    }

    */
