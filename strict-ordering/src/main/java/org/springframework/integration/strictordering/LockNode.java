package org.springframework.integration.strictordering;

import java.io.Serializable;
/**
 * A value object to hold entity lock information
 * @author David Turanski
 *
 */
public class LockNode implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private final String lockName;
	private final String dispatcherName;
	private final String entityKey;

	
	@SuppressWarnings("unused")
	private LockNode(){throw new UnsupportedOperationException();}
	
	public LockNode(String entityKey, String lockName, String dispatcherName){
		this.entityKey = entityKey; 
		this.lockName = lockName;
		this.dispatcherName = dispatcherName; 
	}
	
	public String getLockName() {
		return lockName;
	}
	
 	public String getEntityKey() {
		return entityKey;
	}

	public boolean equals(Object other){
		if (null == other){
			return false;
		}
		
		if (!(other instanceof LockNode)){
			return false;
		}
		
		LockNode otherLockNode = (LockNode)other;
		return this.getKey().equals(otherLockNode.getKey());
	}
	
	public int hashCode(){
	   return getKey().hashCode();
	}
	
	public String toString(){
		return ("entityKey [" + entityKey + "] lockName [" + lockName + "] dispatcherName [" + dispatcherName + "]");
	}

	public String getDispatcherName() {
		return dispatcherName;
	}
	
	public String getKey(){
		return entityKey + ":" + lockName + ":" + dispatcherName;
	}
}
