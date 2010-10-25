package org.springframework.integration.strictordering;

import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
/**
 * Manages queues mapped by entityKey
 * 
 * @author David Turanski
 *
 * @param <K> - The entityKey type
 * @param <T> - The queued item type
 */
public class EntityQueues<K, T> {
	private Map<K,LinkedBlockingQueue<T>> entityQueueMap;
	private int capacity;
	public EntityQueues(){
		entityQueueMap = new HashMap<K,LinkedBlockingQueue<T>>();
	}

	/**
	 * Set the queue capacity
	 * @param capacity
	 */
	public void setCapacity(int capacity){
		this.capacity = capacity;
	}

	/**
	 * 
	 * @param key
	 * @return
	 */
	public Object remove(K key){
		Queue<T> queue = entityQueueMap.get(key);
		if (queue == null ){
			return null;
		} 
		
		Object entity =  queue.remove();
		//Destroy the queue if no elements left.
		if (queue.size() == 0){
			entityQueueMap.remove(key);
		}
		
		return entity;
	}
	
	/**
	 * 
	 * @param key
	 * @return
	 */
	public int size(K key){
		Queue<T> queue = entityQueueMap.get(key);
		if (null == queue){
			return 0;
		}
		return queue.size();
	}
	
	/**
	 * 
	 * @return
	 */
	public Set<K> keySet(){
		return entityQueueMap.keySet();
	}

	/**
	 * 
	 * @param key
	 * @param entity
	 */
	public void add(K key, T entity){
		Queue<T> queue = getQueue(key);
		queue.add(entity);  
	}

	private Queue<T> getQueue(K key) {
		LinkedBlockingQueue<T> queue = entityQueueMap.get(key);

		if (null == queue) {
			queue = (capacity > 0) ? new LinkedBlockingQueue<T>(capacity): new LinkedBlockingQueue<T>();
			entityQueueMap.put(key,queue);
		}
	
		return queue;
	}


}
