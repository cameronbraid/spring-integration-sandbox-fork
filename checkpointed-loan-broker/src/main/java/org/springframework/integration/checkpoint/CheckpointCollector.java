package org.springframework.integration.checkpoint;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.springframework.integration.Message;
/**
 * An aggregator class to generate a single message with a payload containing a collection of {@link Checkpoint}  
 *  
 * @author David Turanski
 *
 */
public class CheckpointCollector {
	private static Logger logger = Logger.getLogger(CheckpointCollector.class);
	
	/**
	 * 
	 * @param messages
	 * @return a collection of {@link Checkpoint} contained in the incoming messages
	 */
	public Object mergeMessages(List<Message<Checkpoint>>messages){
    	List<Checkpoint> checkpoints = new ArrayList<Checkpoint>();	
    
    	logger.info("merging " + messages.size() + " messages...");
    	for (Message<Checkpoint> message:messages){
    		checkpoints.add(message.getPayload());
    		logger.debug(message.getPayload());
    	}
    	logger.info("sending " + checkpoints.size() + " checkpoints...");
    	return checkpoints;
    }
}
