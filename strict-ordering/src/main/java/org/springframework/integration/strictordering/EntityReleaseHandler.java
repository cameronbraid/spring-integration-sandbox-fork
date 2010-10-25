package org.springframework.integration.strictordering;

import org.apache.log4j.Logger;
import org.springframework.integration.Message;
 
import com.gemstone.gemfire.cache.EntryEvent;

/**
 * On release of lock send released entityKey to the dispatcher.
 * 
 * @author David Turanski
 *
 */
public class EntityReleaseHandler {
	private static Logger logger = Logger.getLogger(EntityReleaseHandler.class);
	private Dispatcher dispatcher;
	
	public EntityReleaseHandler(Dispatcher dispatcher){
		this.dispatcher = dispatcher;
	}
	
	public Message<?> handleEvent(EntryEvent<?,?> event){
		
		if (event.getOperation().isDestroy()){
			logger.info("got a cache event" + messageLog(event));
			String key = ((LockNode)event.getOldValue()).getEntityKey();
			return dispatcher.processQueue(key);
		}
		return null;
	}
	
	private String messageLog(EntryEvent<?,?> event) {
		Object key = event.getKey();
		return "[" + key + "=" + event.getOldValue() + "]" + " operation [" + event.getOperation() + "]";
	}

}
