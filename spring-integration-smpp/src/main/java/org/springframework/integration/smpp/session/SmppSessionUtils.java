package org.springframework.integration.smpp.session;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jsmpp.bean.BindType;
import org.jsmpp.bean.NumberingPlanIndicator;
import org.jsmpp.bean.TypeOfNumber;
import org.jsmpp.session.SMPPSession;

public abstract class SmppSessionUtils {

	static private Log log = LogFactory.getLog(SmppSessionUtils.class);

	static public void closeSmppSessionSafely(SMPPSession session) {
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

	static public void connectAndBindSmppSessionSafely(SMPPSession smppSession, String host, int port, BindType bindType, String systemId, String password, String systemType, TypeOfNumber addrTon, NumberingPlanIndicator addrNpi, String addressRange, long timeout) throws Exception {
		if (smppSession != null) {
			if (smppSession.getSessionState().isBound()) {
				log.warn("the session is already bound. Returning without any changes..");
				return;
			}
			smppSession.connectAndBind(host, port, bindType, systemId, password, systemType, addrTon, addrNpi, addressRange, timeout);
		}
	}
}
