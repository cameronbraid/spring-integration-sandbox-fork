package org.springframework.integration.gemfire.inbound.cq;

import com.gemstone.gemfire.cache.query.CqEvent;
import org.springframework.integration.Message;
import org.springframework.integration.annotation.ServiceActivator;

public class CqServiceActivator {

	@ServiceActivator
	public void handleMessage(Message<CqEvent> msg) throws Exception {
		CqEvent cqEvent = msg.getPayload();
		System.out.println( "Received an event from the continuous query adapter: " +cqEvent );
	}
}
