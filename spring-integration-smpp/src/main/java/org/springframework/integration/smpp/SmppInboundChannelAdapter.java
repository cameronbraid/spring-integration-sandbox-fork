package org.springframework.integration.smpp;

import org.jsmpp.session.SMPPSession;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.integration.context.IntegrationObjectSupport;
import org.springframework.integration.endpoint.AbstractEndpoint;
import org.springframework.util.Assert;

/**
 *
 *
 * Supports receiving messages of a payload specified by the SMPP protocol from a <em>short message service center</em> (SMSC).
 *
 *
 * @since 2.1
 * @author Josh Long
 *
 * todo find some way to configure the {@link java.util.concurrent.Executor}s for the JSMPP library
 *
 */
public class SmppInboundChannelAdapter extends AbstractEndpoint {

	private SMPPSession smppSession ;

	@Override
	protected void onInit() throws Exception {
	 Assert.notNull(this.smppSession , "the 'smppSession' property must be set") ;
	}

	public void setSmppSession(SMPPSession smppSession) {
		this.smppSession = smppSession;
	}

	@Override
	protected void doStart() {

	}

	@Override
	protected void doStop() {
	}


}
