package org.springframework.integration.strictordering;

import java.util.Set;

/**
 * 
 * @author David Turanski
 *
 */
public interface EntityLock {
    
	/**
	 * 
	 * @return the dispatcher name. One instance of EntityLock per dispatcher
	 */
	//TODO: Could be something more generic. Provides an additional name space for the locks
	public String getDispatcherName();
     
     /**
      * 
      * @param entityKey
      * @param lockName
      */
     public void lockEntity(String entityKey, String lockName);
     
     /**
      * 
      * @param entityKey
      * @param lockName
      * @return
      */
     public boolean exists(String entityKey, String lockName);
     
     
     /**
      * Test if any lock exists for an entity
      * @param entityKey
      * @return true if any locks exist
      */
     public boolean exists(String entityKey);
     
    /**
     * Release the lock
     * @param entityKey
     * @param lockName
     */
     public void releaseEntity(String entityKey, String lockName);
     
     /**
      * release all locks on an entity
      * @param entityKey
      */
     public void releaseEntity(String entityKey);
      
     /**
      * Replace an existing lock with one or more locks
      * @param entityKey 
      * @param fromLockName - if null, default is dispatcherName
      * @param toLockNames - varargs or String[]
      */
     public void fork(String entityKey, String fromLockName, String ... toLockNames);
     
     /**
      * Find all @{link LockNode}s on an entity
      * @param entityKey
      * @return the LockNodes
      */
     public Set<LockNode> getLocks(String entityKey);
}
