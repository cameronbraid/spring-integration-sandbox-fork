package org.springframework.integration.strictordering;

import org.apache.log4j.Logger;
import org.springframework.integration.Message;

/**
 * On release of lock send released entityKey to the dispatcher.
 * 
 * @author David Turanski
 *
 */
public class EntityLockListener {
	private static Logger logger = Logger.getLogger(EntityLockListener.class);
	private Dispatcher dispatcher;
	
	public EntityLockListener(Dispatcher dispatcher){
		this.dispatcher = dispatcher;
	}
	
	public Message<?> onRelease(LockNode lockNode){
			logger.info("lock released " + lockNode);
			return dispatcher.processQueue(lockNode.getEntityKey());
	}
}
