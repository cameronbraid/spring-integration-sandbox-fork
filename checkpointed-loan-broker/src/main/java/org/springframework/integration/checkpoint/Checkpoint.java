package org.springframework.integration.checkpoint;

import java.io.Serializable;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Map.Entry;
import java.util.Properties;

import org.springframework.integration.Message;
import org.springframework.integration.history.MessageHistory;

/**
 * A value object containing all checkpoint information
 *  
 * @author David Turanski
 *
 */
public class Checkpoint implements Serializable {
	
    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public static enum MessageEvent{PRE_SEND, POST_SEND, PRE_RECEIVE, POST_RECEIVE}
    private String globalTransactionId;
    private String channelName;
    private MessageHistory messageHistory;
    private long timestamp;
    private MessageEvent messageEvent; 
    private Properties properties;
    private static String hostname;
    private static String hostIP;
    private String messageID;
    private String payloadType;
    private Message<?> message;
   
    static {
    	try { InetAddress addr = InetAddress.getLocalHost(); 
 	     hostIP = addr.getHostAddress();
 	     hostname = addr.getHostName(); 
 	  } catch (UnknownHostException e) { 
 		  
 	  } 
    }
 
    /**
     * 
     * @return
     */
	public String getGlobalTransactionId() {
		return globalTransactionId;
	}
	/**
	 * 
	 * @param processInstanceId
	 */
	public void setGlobalTransactionId(String processInstanceId) {
		this.globalTransactionId = processInstanceId;
	}
	
	/**
	 * 
	 * @return
	 */
	public String getChannelName() {
		return channelName;
	}
	
	/**
	 * 
	 * @param channelName
	 */
	public void setChannelName(String channelName) {
		this.channelName = channelName;
	}
	
	/**
	 * 
	 * @return
	 */
	public long getTimestamp() {
		return timestamp;
	}
	
	/**
	 * 
	 * @param timestamp
	 */
	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}
	
	/**
	 * 
	 * @return
	 */
	public MessageEvent getMessageEvent() {
		return messageEvent;
	}

	/**
	 * 
	 * @param messageEvent
	 */
	public void setMessageEvent(MessageEvent messageEvent) {
		this.messageEvent = messageEvent;
	}

	/**
	 * 
	 * @return
	 */
	public String getHostname() {
		return hostname;
	}
	
    /**
     * 
     * @return
     */
	public String getHostIP() {
		return hostIP;
	}

	/**
	 * 
	 * @return optional metadata related to the transaction
	 */
	public Properties getProperties() {
		return properties;
	}
	
	/**
	 * 
	 * @param properties optional metadata related to the transaction
	 */
	public void setProperties(Properties properties) {
		this.properties = properties;
	}
	
	/**
	 * 
	 * @return
	 */
	public MessageHistory getMessageHistory() {
		return messageHistory;
	}

	/**
	 * 
	 * @param messageHistory
	 */
	public void setMessageHistory(MessageHistory messageHistory) {
		this.messageHistory = messageHistory;
	}

	/**
	 * 
	 * @return the original message if available
	 */
	public Message<?> getMessage() {
		return message;
	}

	/**
	 * 
	 * @param message
	 */
	public void setMessage(Message<?> message) {
		this.message = message;
	}
  
	/**
	 * 
	 * @return
	 */
	public String getMessageID() {
		return messageID;
	}

	/**
	 * 
	 * @param messageID
	 */
	public void setMessageID(String messageID) {
		this.messageID = messageID;
	}
	

	/**
	 * 
	 * @return
	 */
	public String getPayloadType() {
		return payloadType;
	}

	/**
	 * 
	 * @param payloadType
	 */
	public void setPayloadType(String payloadType) {
		this.payloadType = payloadType;
	}

	/**
	 * 
	 */
	public String toString(){
		StringBuilder sb = new StringBuilder();
		sb.append(" globalTransactionID [").append(globalTransactionId).append("]")
		.append(" timestamp [").append(timestamp).append("]")
		.append(" channelName [").append(channelName).append("]")
		.append(" messageEvent [").append(messageEvent).append("]")
		.append(" messageID [").append(messageID).append("]")
		.append(" payloadType [").append(payloadType).append("]")
		.append(" messageHistory [").append(displayMessageHistory()).append("]")
		.append(" hostname [").append(hostname).append("]")
		.append(" hostIP [").append(hostIP).append("]")
		.append("\n");
		if ( null != properties ){
			sb.append("properties:\n");
			for (Entry<Object, Object> property :properties.entrySet()){
				sb.append("key [").append(property.getKey()).append("] value [").append(property.getValue()).append("]\n");	
			}
		}
		if ( null != message ) {
			sb.append("message: [").append(message).append("]\n");
		}
		return sb.toString();
	}

	private String displayMessageHistory() {
		if ( null != messageHistory ){
			return messageHistory.toString();
		} else {
			return("");
		}
	}
    
}
