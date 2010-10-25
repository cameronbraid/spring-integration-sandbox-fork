package org.springframework.integration.strictordering;



import org.apache.log4j.Logger;
import org.springframework.integration.Message;
import org.springframework.integration.strictordering.entitykey.EntityKeyExtractor;
import org.springframework.integration.support.MessageBuilder;
/**
 * Enforces strict ordering by using an {@link EntityLock}. This is configured as a {@link MessageRouter}. If the entityKey
 * extracted from the Message is not locked, the message will be simply routed to the outputChannel. If a lock exists, the 
 * Message will be held in an internal queue. When all locks on the entity are released, the next queued message will be dispatched. 
 * 
 * @author David Turanski
 *
 */
public class Dispatcher   {
	private static final String DISPATCHER = "dispatcher";

	@SuppressWarnings("rawtypes")
	// Manages internal queues
	private EntityQueues entityQueues = new EntityQueues();
    
	//A distributed lock implementation
	private final EntityLock  entityLock;
	
	private final String outputChannelName;
	 
	private static Logger logger = Logger.getLogger(Dispatcher.class);
	
	//A strategy interface used to extract the entityKey from the message. If not set, the payload will be used as the key
	private EntityKeyExtractor<Message<?>,?> entityKeyExtractor;
	
	/**
	 * 
	 * @param entityLock
	 * @param outputChannelName
	 */
	public Dispatcher(EntityLock  entityLock, String outputChannelName){
		this.entityLock = entityLock;
		this.outputChannelName = outputChannelName;
	}
	
	/**
	 * Dispatch or queue the Message
	 * @param message
	 * @return
	 */
	public String dispatch(Message<?> message){
		
		logger.debug("got message " + message);
		Object key = extractKey(message);
		if (! entityLock.exists((String)(key)) ){
			entityLock.lockEntity((String)key, DISPATCHER);
			logger.debug ("no lock on entity - processing message "+ message);
			return outputChannelName;
		} else {
			logger.debug("entity locked - queuing message "+ message);
			queue(message);
		}
		return null;
	}

	/**
	 * Process the next queued message
	 * @param entityKey
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public Message<?> processQueue(String entityKey){
        Message<?> queuedMessage = null;
       
        if (!entityLock.exists(entityKey)){
           //Check if any messages are queued
		   if (entityQueues.size(entityKey) > 0 ){
				logger.info("getting next message from queue "+"[" + entityKey + "]");
				queuedMessage = (Message<?>)entityQueues.remove(entityKey);
		   }
        } 
		return queuedMessage;
	}
	
	/**
	 * 
	 * @param entityKeyExtractor
	 */
	public void setEntityKeyExtractor(EntityKeyExtractor<Message<?>, ?> entityKeyExtractor){
		this.entityKeyExtractor = entityKeyExtractor;
	}
	
	private Object extractKey(Message<?> message) {
		return (null == entityKeyExtractor) ? message.getPayload() : entityKeyExtractor.getKey(message);
	}

	@SuppressWarnings("unchecked")
	private void queue(Message<?> message) {
		Message<?> queuedMessage = MessageBuilder.fromMessage(message).setHeaderIfAbsent("queued",true).build();
		entityQueues.add(extractKey(message),queuedMessage);
	}
}
