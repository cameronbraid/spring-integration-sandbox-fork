package org.springframework.integration.smpp.session;

import org.jsmpp.bean.AlertNotification;
import org.jsmpp.bean.DataSm;
import org.jsmpp.bean.DeliverSm;
import org.jsmpp.extra.ProcessRequestException;
import org.jsmpp.session.DataSmResult;
import org.jsmpp.session.MessageReceiverListener;
import org.jsmpp.session.Session;

/**
 * Simple abstract class so we don't always have to implement every method.
 *
 * @author Josh Long
 * @since 2.1
 */
public class AbstractMessageReceiverListener implements MessageReceiverListener {
	public void onAcceptDeliverSm(DeliverSm deliverSm) throws ProcessRequestException {
	}

	public void onAcceptAlertNotification(AlertNotification alertNotification) {
	}

	public DataSmResult onAcceptDataSm(DataSm dataSm, Session source) throws ProcessRequestException {
		return null;
	}
}
