package org.springframework.integration.gemfire.store.messagegroupstore;

import org.apache.commons.lang.StringUtils;

import org.springframework.integration.Message;
import org.springframework.integration.annotation.ServiceActivator;

import org.springframework.stereotype.Component;

import java.util.Collection;


/**
 * @author Josh Long
 */
@Component
public class MessageGroupStoreActivator {
	
    @ServiceActivator
    public void activate(Message<Collection<Object>> msg) throws Throwable {
        Collection<Object> payloads = msg.getPayload();

        System.out.println(StringUtils.repeat("-", 100));
        System.out.println(StringUtils.join(payloads, ","));

    }
}
