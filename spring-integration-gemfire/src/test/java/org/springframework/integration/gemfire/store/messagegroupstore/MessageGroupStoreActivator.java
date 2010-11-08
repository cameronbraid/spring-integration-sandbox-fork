package org.springframework.integration.gemfire.store.messagegroupstore;

import org.springframework.core.style.ToStringCreator;
import org.springframework.integration.Message;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.stereotype.Component;

/**
 * @author Josh Long
 */
@Component
public class MessageGroupStoreActivator {

	@ServiceActivator 
	public void activate ( Message<?> msg ) throws Throwable {
		 System.out.println(new  ToStringCreator( msg ));
	}
}
