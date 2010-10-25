package org.springframework.integration.strictordering.gemfire;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;
import org.springframework.integration.strictordering.EntityLock;
import org.springframework.integration.strictordering.LockNode;
import org.springframework.util.Assert;

import com.gemstone.gemfire.cache.Region;

/**
 * An implementation of @{link EntityLock} backed by Gemfire. 
 * This creates a separate namespace for the dispatcherName to allow for multiple dispatchers
 * 
 * @author David Turanski
 *
 */
public class GemfireEntityLock implements EntityLock   {

 
	private final Region<String,LockNode> region;
	private static Logger logger = Logger.getLogger(GemfireEntityLock.class);
	private final String dispatcherName; 
	
	@SuppressWarnings("unused")
	private GemfireEntityLock(){
		region = null;
		dispatcherName = null;
		 
	}
	
	@SuppressWarnings("unchecked")
	public GemfireEntityLock( @SuppressWarnings("rawtypes") Region region, String dispatcherName){
		this.region = ( Region<String,LockNode>)region;
		this.dispatcherName = dispatcherName;	
	}

    /*
     * (non-Javadoc)
     * @see org.springframework.integration.strictordering.EntityLock#lockEntity(org.springframework.integration.strictordering.LockNode)
     */
	@Override
	public void lockEntity(String entityKey, String lockName) {
		LockNode lockNode = new LockNode(entityKey,lockName,dispatcherName);
		region.put(lockNode.getKey(),lockNode);
	}
	
    /*
     * (non-Javadoc)
     * @see org.springframework.integration.strictordering.EntityLock#exists(java.lang.String)
     */
	@Override
	public boolean exists(String entityKey) {
		Collection<LockNode> lockNodes = region.values();
		for(LockNode lockNode: lockNodes){
			if (lockNode.getEntityKey().equals(entityKey) && lockNode.getDispatcherName().equals(dispatcherName)){
				return true;
			}
		}
		return false;
	}
	
    /*
     * (non-Javadoc)
     * @see org.springframework.integration.strictordering.EntityLock#releaseEntity(org.springframework.integration.strictordering.LockNode)
     */
	@Override
	public void releaseEntity(String entityKey, String lockName) {
		removeLock( new LockNode(entityKey, lockName, dispatcherName) );
	}
	
	 /*
	  * (non-Javadoc)
	  * @see org.springframework.integration.strictordering.EntityLock#getLocks(java.lang.String)
	  */
	@Override
	public Set<LockNode> getLocks(String entityKey) {
		Collection<LockNode> lockNodes = region.values();
		Set<LockNode> results = new HashSet<LockNode>();
		for(LockNode lockNode: lockNodes){
			if (lockNode.getEntityKey().equals(entityKey) && lockNode.getDispatcherName().equals(dispatcherName)){
				results.add(lockNode);
			}
		}
		return results;
//TODO: May not be worth a query. This runs slower without any indexing and the number of entries in the region should be relatively small		
//		SelectResults<LockNode> results = null;
//		try {
//			results = region.query(" dispatcherName = '" + dispatcherName + "' and entityKey = '" + entityKey  + "'");
//		} catch (FunctionDomainException e) {
//			throw new RuntimeException(e);
//		} catch (TypeMismatchException e) {
//			throw new RuntimeException(e);
//		} catch (NameResolutionException e) {
//			throw new RuntimeException(e);
//		} catch (QueryInvocationTargetException e) {
//			throw new RuntimeException(e);
//		}
//		
//		return results.asSet();
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.springframework.integration.strictordering.EntityLock#releaseEntity(java.lang.String)
	 */
	@Override
	public synchronized void releaseEntity(String entityKey) {
		for (LockNode lockNode:getLocks(entityKey)){
			this.removeLock(lockNode);
		}
	}
  
	/*
	 * (non-Javadoc)
	 * @see org.springframework.integration.strictordering.EntityLock#fork(java.lang.String, java.lang.String[])
	 */
	@Override
	public void fork(String entityKey, String fromLockName, String... toLockNames) {
		if (fromLockName == null) {
			fromLockName = dispatcherName;
		}
		Assert.isTrue(exists(entityKey, fromLockName),  fromLockName + " does not have a lock for entity [" + entityKey + "]" );
		 for (String lockName: toLockNames){
			 lockEntity(entityKey, lockName);
		 }
		 removeLock( new LockNode(entityKey, fromLockName, dispatcherName) );
	}
	
	protected final void removeLock(LockNode lockNode) {
		Assert.notNull(lockNode, "lockNode cannot be null");
		logger.debug("entity [" + lockNode.getEntityKey() + "] is being removed by [" + lockNode.getLockName() + "]");
	 	region.destroy(lockNode.getKey()); 
	}

	@Override
	public String getDispatcherName() {
		return dispatcherName;
	}

	@Override
	public boolean exists(String entityKey, String lockName) {
		LockNode lockNode = new LockNode(entityKey, lockName, dispatcherName);
		logger.debug("exists [" + lockNode.getKey() + "] ?");
		return region.containsKey(lockNode.getKey());
	}	
}
	
	
  

 
