package org.springframework.integration.smpp;

import org.jsmpp.InvalidResponseException;
import org.jsmpp.PDUException;
import org.jsmpp.bean.*;
import org.jsmpp.extra.NegativeResponseException;
import org.jsmpp.extra.ResponseTimeoutException;
import org.jsmpp.session.BindParameter;
import org.jsmpp.session.SMPPSession;
import org.jsmpp.util.AbsoluteTimeFormatter;
import org.jsmpp.util.TimeFormatter;

import java.io.IOException;
import java.util.Date;

/**
 * @author Josh Long
 *         <p/>
 *         scratchpad
 */
public class Main {
	public static void main(String[] args) {
		SMPPSession session = new SMPPSession();
		try {
			int port = 2775;
			String host ="localhost";
			session.connectAndBind( host,port , new BindParameter(BindType.BIND_TX, "smppclient1", "password", "cp", TypeOfNumber.UNKNOWN, NumberingPlanIndicator.UNKNOWN, null));
		} catch (IOException e) {
			System.err.println("Failed connect and bind to host");
			e.printStackTrace();
		}

		try {
			TimeFormatter  timeFormatter = new AbsoluteTimeFormatter();
			String messageId = session.submitShortMessage("CMT", TypeOfNumber.INTERNATIONAL, NumberingPlanIndicator.UNKNOWN, "1616", TypeOfNumber.INTERNATIONAL, NumberingPlanIndicator.UNKNOWN, "628176504657", new ESMClass(), (byte) 0, (byte) 1,  timeFormatter.format(new Date()), null, new RegisteredDelivery(SMSCDeliveryReceipt.DEFAULT), (byte) 0, new GeneralDataCoding(false, true, MessageClass.CLASS1, Alphabet.ALPHA_DEFAULT), (byte) 0, "jSMPP simplify SMPP on Java platform".getBytes());
			System.out.println("Message submitted, message_id is " + messageId);
		} catch (PDUException e) {
			// Invalid PDU parameter
			System.err.println("Invalid PDU parameter");
			e.printStackTrace();
		} catch (ResponseTimeoutException e) {
			// Response timeout
			System.err.println("Response timeout");
			e.printStackTrace();
		} catch (InvalidResponseException e) {
			// Invalid response
			System.err.println("Receive invalid respose");
			e.printStackTrace();
		} catch (NegativeResponseException e) {
			// Receiving negative response (non-zero command_status)
			System.err.println("Receive negative response");
			e.printStackTrace();
		} catch (IOException e) {
			System.err.println("IO error occur");
			e.printStackTrace();
		}

		session.unbindAndClose();
	}
}
