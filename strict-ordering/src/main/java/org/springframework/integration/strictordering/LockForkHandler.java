package org.springframework.integration.strictordering;

import org.apache.log4j.Logger;
import org.springframework.integration.Message;
import org.springframework.integration.strictordering.entitykey.EntityKeyExtractor;

/**
 * A convenient message handler for forking an entity lock
 * 
 * @author David Turanski
 *
 */
public class LockForkHandler {
	private final EntityLock  entityLock;
	private final String[] lockNames;
	private String fromLockName; 
	private EntityKeyExtractor<Message<?>,?> entityKeyExtractor;
	@SuppressWarnings("unused")
	private static Logger logger = Logger.getLogger(LockForkHandler.class);

	/**
	 * 
	 * @param entityLock
	 * @param lockNames
	 */
	public LockForkHandler(EntityLock  entityLock, String[] lockNames){
		this.entityLock = entityLock;
		this.lockNames = lockNames;
	}
	
	/**
	 * 
	 * @param message
	 * @return
	 */
    public  Message<?> forkLock(Message<?> message ){
    	Object entityKey = extractKey(message);
    	entityLock.fork((String)entityKey, fromLockName, lockNames );
    	return message;
    }
    
   /**
    * 
    * @param entityKeyExtractor
    */
	public void setEntityKeyExtractor(EntityKeyExtractor<Message<?>, ?> entityKeyExtractor){
		this.entityKeyExtractor = entityKeyExtractor;
	}
	
	/**
	 * 
	 * @param fromLockName - optional. Will use the dispatcherName if not set
	 */
	public void setFromLockName(String fromLockName) {
		this.fromLockName = fromLockName;
	}
	
	private Object extractKey(Message<?> message) {	
		return (null == entityKeyExtractor) ? message.getPayload() : entityKeyExtractor.getKey(message);
	}
	
	
}
