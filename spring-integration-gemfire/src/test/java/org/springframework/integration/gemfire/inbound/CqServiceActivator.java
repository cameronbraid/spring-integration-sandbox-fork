package org.springframework.integration.gemfire.inbound;

import org.springframework.integration.Message;
import org.springframework.integration.annotation.ServiceActivator;

public class CqServiceActivator {
	@ServiceActivator
	public void handleMessage(Message<?> msg) throws Exception {
		System.out.println( "Received an event from the continuous query adapter: " +msg);
	}
}
