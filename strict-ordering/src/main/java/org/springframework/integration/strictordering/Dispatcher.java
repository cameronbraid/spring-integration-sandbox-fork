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
public class Dispatcher  {
	private static final String QUEUED_HEADER_KEY = "$queued.for.strict.order";

	private static final String DISPATCHER = "dispatcher";

	@SuppressWarnings("rawtypes")
	// Manages internal queues
	private EntityQueues entityQueues = new EntityQueues();
    
	//A distributed lock implementation
	private final EntityLock  entityLock;

	private static Logger logger = Logger.getLogger(Dispatcher.class);
	
	//A strategy interface used to extract the entityKey from the message. If not set, the payload will be used as the key
	private EntityKeyExtractor<Message<?>,?> entityKeyExtractor;
	
	/**
	 * 
	 * @param entityLock
	 * @param outputChannelName
	 */
	public Dispatcher(EntityLock  entityLock){
		this.entityLock = entityLock;
		
	}

	/**
	 * Dispatch or queue the Message
	 * @param message
	 * @return
	 */
	
	@SuppressWarnings("unchecked")
	public synchronized Message<?> dispatch(Message<?> message) {
	
		logger.debug("got message " + message);
		String key = (String)extractKey(message);
	    Message<?> transformedMessage = message;
	    
	    /*
	     * Message removed from queue. Make sure not to re-queue 
	     */
	    if ( message.getHeaders().get(QUEUED_HEADER_KEY) != null ){
		    logger.debug ("processing queued message "+ transformedMessage);
	    	entityLock.lockEntity(key, DISPATCHER);
	    	return transformedMessage;
	    }
	    
	    /*
	     * Message from original producer. It may be that the lock is clear but the queue has
	     * not processed yet
	     */
	    if ( ! entityLock.exists(key) && entityQueues.size(key) == 0 ){
			 logger.debug ("no lock on entity - processing message "+ transformedMessage);
			 entityLock.lockEntity(key, DISPATCHER);
	    } else {
			  logger.debug("entity locked - queuing message "+ message);
			  queue(message);
			  transformedMessage = null;
		}
		  
		return transformedMessage;
	}

	/**
	 * Process the next queued message if lock is cleared
	 * @param entityKey
	 * @return
	 */
	public synchronized Message<?> processQueue(String entityKey){
        Message<?> queuedMessage = null;
        if (!entityLock.exists(entityKey)){
          queuedMessage = nextMessage(entityKey);
        } 
		return queuedMessage;
	}
	
	@SuppressWarnings("unchecked")
	private Message<?> nextMessage(String entityKey){
		Message<?> queuedMessage = null;
		if (entityQueues.size(entityKey) > 0 ){
			logger.info("getting next message from queue "+"[" + entityKey + "]");
			queuedMessage = (Message<?>)entityQueues.remove(entityKey);
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
		Message<?> queuedMessage = MessageBuilder.fromMessage(message).setHeaderIfAbsent(QUEUED_HEADER_KEY,true).build();
		entityQueues.add(extractKey(message),queuedMessage);
	}


}
