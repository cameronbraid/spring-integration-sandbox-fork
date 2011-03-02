package org.springframework.integration.smpp.session;

import org.jsmpp.InvalidResponseException;
import org.jsmpp.PDUException;
import org.jsmpp.bean.*;
import org.jsmpp.extra.NegativeResponseException;
import org.jsmpp.extra.ResponseTimeoutException;
import org.jsmpp.extra.SessionState;
import org.jsmpp.session.*;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import java.io.IOException;

/**
 * Adapts to the {@link ClientSession} API, while also providing the callbacks for the Spring container
 *
 * @author Josh Long
 * @since 2.1
 */
public class ExtendedSmppSessionAdaptingDelegate implements ExtendedSmppSession, InitializingBean, DisposableBean {

	private final DelegatingMessageReceiverListener delegatingMessageReceiverListener = new DelegatingMessageReceiverListener();

	private ClientSession clientSession;

	public ClientSession getTargetClientSession() {
		return this.clientSession;
	}

	public void destroy() throws Exception {
		if (clientSession instanceof DisposableBean)
			((DisposableBean) clientSession).destroy();
	}

	public void afterPropertiesSet() throws Exception {

		if (clientSession instanceof SMPPSession)
			((SMPPSession) clientSession).setMessageReceiverListener(delegatingMessageReceiverListener);

		if (clientSession instanceof InitializingBean)
			((InitializingBean) clientSession).afterPropertiesSet();
	}

	public ExtendedSmppSessionAdaptingDelegate(ClientSession clientSession) {
		this.clientSession = clientSession;
	}

	public void addMessageReceiverListener(MessageReceiverListener messageReceiverListener) {
		this.delegatingMessageReceiverListener.addMessageReceiverListener(messageReceiverListener);
	}

	public String submitShortMessage(String serviceType, TypeOfNumber sourceAddrTon, NumberingPlanIndicator sourceAddrNpi,
																	 String sourceAddr, TypeOfNumber destAddrTon, NumberingPlanIndicator destAddrNpi,
																	 String destinationAddr, ESMClass esmClass, byte protocolId, byte priorityFlag, String scheduleDeliveryTime, String validityPeriod, RegisteredDelivery registeredDelivery, byte replaceIfPresentFlag, DataCoding dataCoding, byte smDefaultMsgId, byte[] shortMessage, OptionalParameter... optionalParameters) throws PDUException, ResponseTimeoutException, InvalidResponseException, NegativeResponseException, IOException {
		return clientSession.submitShortMessage(serviceType, sourceAddrTon, sourceAddrNpi, sourceAddr, destAddrTon, destAddrNpi, destinationAddr, esmClass, protocolId, priorityFlag, scheduleDeliveryTime, validityPeriod, registeredDelivery, replaceIfPresentFlag, dataCoding, smDefaultMsgId, shortMessage, optionalParameters);
	}

	public SubmitMultiResult submitMultiple(String serviceType, TypeOfNumber sourceAddrTon, NumberingPlanIndicator sourceAddrNpi, String sourceAddr, Address[] destinationAddresses, ESMClass esmClass, byte protocolId, byte priorityFlag, String scheduleDeliveryTime, String validityPeriod, RegisteredDelivery registeredDelivery, ReplaceIfPresentFlag replaceIfPresentFlag, DataCoding dataCoding, byte smDefaultMsgId, byte[] shortMessage, OptionalParameter[] optionalParameters) throws PDUException, ResponseTimeoutException, InvalidResponseException, NegativeResponseException, IOException {
		return clientSession.submitMultiple(
				serviceType, sourceAddrTon, sourceAddrNpi, sourceAddr, destinationAddresses, esmClass, protocolId, priorityFlag, scheduleDeliveryTime, validityPeriod, registeredDelivery, replaceIfPresentFlag, dataCoding, smDefaultMsgId, shortMessage, optionalParameters
		);
	}

	public QuerySmResult queryShortMessage(String messageId, TypeOfNumber sourceAddrTon, NumberingPlanIndicator sourceAddrNpi, String sourceAddr) throws PDUException, ResponseTimeoutException, InvalidResponseException, NegativeResponseException, IOException {
		return clientSession.queryShortMessage(messageId, sourceAddrTon, sourceAddrNpi, sourceAddr);
	}

	public void cancelShortMessage(String serviceType, String messageId, TypeOfNumber sourceAddrTon, NumberingPlanIndicator sourceAddrNpi, String sourceAddr,
																 TypeOfNumber destAddrTon, NumberingPlanIndicator destAddrNpi, String destinationAddress) throws PDUException, ResponseTimeoutException, InvalidResponseException, NegativeResponseException, IOException {
		clientSession.cancelShortMessage(serviceType, messageId, sourceAddrTon, sourceAddrNpi, sourceAddr, destAddrTon, destAddrNpi, destinationAddress);
	}

	public void replaceShortMessage(String messageId, TypeOfNumber sourceAddrTon, NumberingPlanIndicator sourceAddrNpi, String sourceAddr, String scheduleDeliveryTime, String validityPeriod, RegisteredDelivery registeredDelivery, byte smDefaultMsgId, byte[] shortMessage) throws PDUException, ResponseTimeoutException, InvalidResponseException, NegativeResponseException, IOException {
		clientSession.replaceShortMessage(messageId, sourceAddrTon, sourceAddrNpi, sourceAddr, scheduleDeliveryTime, validityPeriod, registeredDelivery, smDefaultMsgId, shortMessage);
	}

	public DataSmResult dataShortMessage(String serviceType, TypeOfNumber sourceAddrTon, NumberingPlanIndicator sourceAddrNpi, String sourceAddr, TypeOfNumber destAddrTon, NumberingPlanIndicator destAddrNpi, String destinationAddr, ESMClass esmClass, RegisteredDelivery registeredDelivery, DataCoding dataCoding, OptionalParameter... optionalParameters) throws PDUException, ResponseTimeoutException, InvalidResponseException, NegativeResponseException, IOException {
		return clientSession.dataShortMessage(serviceType, sourceAddrTon, sourceAddrNpi, sourceAddr, destAddrTon, destAddrNpi, destinationAddr, esmClass, registeredDelivery, dataCoding, optionalParameters);
	}

	public String getSessionId() {
		return clientSession.getSessionId();
	}

	public void setEnquireLinkTimer(int enquireLinkTimer) {
		clientSession.setEnquireLinkTimer(enquireLinkTimer);
	}

	public int getEnquireLinkTimer() {
		return clientSession.getEnquireLinkTimer();
	}

	public void setTransactionTimer(long transactionTimer) {
		clientSession.setTransactionTimer(transactionTimer);
	}

	public long getTransactionTimer() {
		return clientSession.getTransactionTimer();
	}

	public SessionState getSessionState() {
		return clientSession.getSessionState();
	}

	public void addSessionStateListener(SessionStateListener l) {
		clientSession.addSessionStateListener(l);
	}

	public void removeSessionStateListener(SessionStateListener l) {
		clientSession.removeSessionStateListener(l);
	}

	public long getLastActivityTimestamp() {
		return clientSession.getLastActivityTimestamp();
	}

	public void close() {
		clientSession.close();
	}

	public void unbindAndClose() {
		clientSession.unbindAndClose();
	}
}
