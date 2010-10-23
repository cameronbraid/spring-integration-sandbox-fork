package org.springframework.integration.checkpoint;

import java.util.List;
import java.util.Properties;
import java.util.UUID;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.integration.Message;
import org.springframework.integration.MessageChannel;
import org.springframework.integration.context.NamedComponent;
import org.springframework.integration.channel.interceptor.WireTap;
import org.springframework.integration.checkpoint.Checkpoint.MessageEvent;
import org.springframework.integration.core.MessageSelector;
import org.springframework.integration.history.MessageHistory;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.util.PatternMatchUtils;
import org.springframework.util.StringUtils;


 /**
  * A subclass of {@link WireTap} for generating checkpoint messages
  * to a message channel for transaction monitoring. 
  * 
  * @author David Turanski
  *
  */

public abstract class AbstractCheckpointWireTap extends WireTap implements InitializingBean {
    
	private final MessageChannel checkpointChannel; 
	private long timeout;
	
	private String transactionIdHeader;
	
	private boolean includeMessage;
	
	private String pattern;
	
	private List<MessageEvent> enabledMessageEvents;
	
	protected Logger logger = Logger.getLogger(getClass()); 
	
	public AbstractCheckpointWireTap(MessageChannel channel) {
		super(channel);
		checkpointChannel = channel;
	}
	
	/**
	 * 
	 * @param channel
	 * @param selector
	 */
	public AbstractCheckpointWireTap(MessageChannel channel, MessageSelector selector) {
		super(channel, selector);
		checkpointChannel = channel;
	}

	@Override
	public final Message<?> preSend(Message<?> message, MessageChannel messageChannel) {
		logger.debug("PRE_SEND:"+ this.getChannelName(messageChannel));
		Message<?> enrichedMessage = handleCheckpoint(message, messageChannel, MessageEvent.PRE_SEND); 
		return enrichedMessage;
	}


	/**
	 * @param sent - true if message was sent successfully, false if could not send for a non-fatal reason
	 */
	@Override
	public final void postSend(Message<?> message, MessageChannel messageChannel, boolean sent) {
		handleCheckpoint(message, messageChannel, MessageEvent.POST_SEND);
	}	

	//TODO: Implement PRE_RECEIVE checkpointing on pollable channels
	@Override
	public final boolean preReceive(MessageChannel channel) {
		logger.debug("PRE_RECEIVE:");
		return true;
	}
	//TODO: Implement postReceive checkpointing on pollable channels
	@Override
	public final Message<?> postReceive(Message<?> message, MessageChannel channel) {	 
		logger.debug("postReceive:"  + message.getHeaders());
		return message;
	}
	
	/**
	 * 
	 * @return the key used to carry the global transaction ID in the message header
	 */
	public String getTransactionIdHeader() {
		return transactionIdHeader;
	}
    /**
     * 
     * @param transactionIdHeader the key used to carry the global transaction ID in the message header 
     */
	public void setTransactionIdHeader(String transactionIdHeader) {
		this.transactionIdHeader = transactionIdHeader;
	}

    /**
     * 
     * @return message events @see {Checkpoint.MessageEvent} for which checkpoints will be generated
     */
	public List<MessageEvent> getEnabledMessageEvents() {
		return enabledMessageEvents;
	}

	/**
	 * 
	 * @param enabledMessageEvents message events {@link Checkpoint.MessageEvent} for which checkpoints will be generated
	 */
	public void setEnabledMessageEvents(List<MessageEvent> enabledMessageEvents) {
		this.enabledMessageEvents = enabledMessageEvents;
	}

	private Message<?> handleCheckpoint(Message<?> message, MessageChannel messageChannel, MessageEvent me){
		Message<Checkpoint> checkpointMessage;
		Message<?> enrichedMessage = message;
	    String channelName = getChannelName(messageChannel);
		if (checkpointEnabled(channelName) && messageEventEnabled(me) ){

			logger.debug("creating checkpoint on " + channelName);

			enrichedMessage = initializeTransactionIfNecessary(message);

			checkpointMessage = buildCheckpointMessage(messageChannel, enrichedMessage, me);

			if (timeout > 0){
				checkpointChannel.send(checkpointMessage,timeout);
			} else {
				checkpointChannel.send(checkpointMessage);
			}
		}		
		return enrichedMessage;
	}

    private boolean messageEventEnabled(MessageEvent me) {
    	boolean enabled = true;
		if ( null != enabledMessageEvents){
			enabled = enabledMessageEvents.contains(me);
		}
		return enabled;
	}

	@Override
    public final void setTimeout(long timeout){
	  super.setTimeout(timeout);
	  this.timeout = timeout;
   }

	/**
	 * 
	 * @return true if the entire message is copied to the {@link Checkpoint}
	 */
	public boolean isIncludeMessage() {
		return includeMessage;
	}
	
    /**
     * 
     * @param includeMessage true if the entire message is copied to the {@link Checkpoint}
     */
	public void setIncludeMessage(boolean includeMessage) {
		this.includeMessage = includeMessage;
	}
    /**
     * Set a pattern to filter checkpoint on channels whose name matches the pattern. By default a checkpoint is generated for 
     * all intercepted channels
     * @param pattern a simple wildcard pattern
     */
	public void setPattern(String pattern) {
		this.pattern = pattern;
	}

	private Message<?> initializeTransactionIfNecessary(Message<?> message) {
		return MessageBuilder.fromMessage(message).setHeaderIfAbsent(transactionIdHeader, UUID.randomUUID()).build();
	}
	
	private Message<Checkpoint> buildCheckpointMessage(MessageChannel channel, Message<?> message, MessageEvent me) {
		 
		 return MessageBuilder.withPayload(buildCheckpoint(channel, message,me))
		 .setHeaderIfAbsent(transactionIdHeader,message.getHeaders().get(transactionIdHeader))
		 .build();
	}
    
	/**
	 * add message values as properties in a checkpoint
	 * @param message
	 * @return
	 */
	protected abstract Properties addCheckpointProperties(Message<?> message);
	
	private Checkpoint buildCheckpoint(MessageChannel channel,  Message<?> message, MessageEvent me){
		Checkpoint checkpoint = new Checkpoint();
		
		checkpoint.setChannelName(getChannelName(channel));		 
		
		checkpoint.setGlobalTransactionId(message.getHeaders().get(transactionIdHeader).toString());
		
		checkpoint.setMessageID(message.getHeaders().getId().toString());
		
		MessageHistory history = MessageHistory.read(message);
		
		if ( null != history ){
		  checkpoint.setMessageHistory(history);
		}
		
		checkpoint.setPayloadType(message.getPayload().getClass().getName());
		
		checkpoint.setMessageEvent(me);
	
		checkpoint.setTimestamp((Long)message.getHeaders().getTimestamp());
		
		
		if (includeMessage){
			checkpoint.setMessage(message);
		}
		
		Properties checkpointProperties = addCheckpointProperties(message);
		if (null != checkpointProperties){
			if (null == checkpoint.getProperties()){
				checkpoint.setProperties(checkpointProperties);
			} else {
				checkpoint.getProperties().putAll(checkpointProperties);
			}
		}
		return checkpoint;
	}
	
	private boolean checkpointEnabled(String componentName){
		if (!StringUtils.hasText(pattern)){
			return true;
		} else {
			return PatternMatchUtils.simpleMatch(pattern,componentName);
		}
	}
	
	
	public void afterPropertiesSet() throws Exception {
		if (!StringUtils.hasText(transactionIdHeader)){
			throw new IllegalStateException("transactionIdHeader is a required property");
		}
	}
	
	protected String getChannelName(MessageChannel channel){
		return ((NamedComponent)channel).getComponentName();
	}
	
}
