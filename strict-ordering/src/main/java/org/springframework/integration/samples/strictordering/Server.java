package org.springframework.integration.samples.strictordering;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.log4j.Logger;
import org.springframework.integration.Message;
import org.springframework.integration.strictordering.EntityLock;
import org.springframework.integration.strictordering.entitykey.EntityKeyExtractor;
import org.springframework.util.StopWatch;

public class Server {
	private EntityLock entityLock;
	private int delay;
	private static Logger logger = Logger.getLogger(Server.class);
	private PrintWriter writer;
	private boolean strictOrdering;
	private int maxMessages;
	private StopWatch stopWatch;
	private AtomicInteger numMessages;
	private EntityKeyExtractor<Message<?>,?> entityKeyExtractor; 
	private final int serverId;
	private final Random random;

	public Server(int serverId) {
		this.serverId = serverId;
		this.random = new Random();
	}
	
	public Server (){
		this(1);
	}
	
	public void init(Map<String,Object> testParameters) throws IOException {
		String fileName = (String)testParameters.get("process.filename." + serverId);
		strictOrdering = (Boolean)testParameters.get("strict.ordering");
		maxMessages = (Integer)testParameters.get("max.messages");
		numMessages = new AtomicInteger(0);
		
		logger.info("creating file [" + fileName + "]");
		File file = new File(fileName);
		if (file.exists()){
			file.delete();
			file.createNewFile();		 	
		}
	
		writer = new PrintWriter(new FileOutputStream(file),true); 	
		stopWatch = new StopWatch();
		stopWatch.start();
	}
	
	/**
	 * Stream messages to a file. Release the lock
	 * @param message
	 * @throws FileNotFoundException
	 */
	public void process(Message<String> message) throws FileNotFoundException{
		String serverName = "server" + serverId;
	 	logger.info(serverName + " processing message [" + message.getPayload() + "] sequence [" + message.getHeaders().get("sequence") + "]");
	    int delayFor = delay();
	   
	    logger.info("delayed for "+ delayFor + " ms");
	   
	    writer.println(message.getPayload() + "," + message.getHeaders().get("sequence"));
 
	   if ( null != entityLock && strictOrdering){
		 try {
            entityLock.releaseEntity((String)extractKey(message),serverName );
		 } catch (Exception e) {
			 logger.error(e.getMessage(),e);
		 }
	   }
	   if (numMessages.incrementAndGet() == maxMessages ){
		   stopWatch.stop();
		   logger.info(" elapsed time [" + (strictOrdering ? "ordered]" : "unordered]") + stopWatch.getTotalTimeMillis() + " ms"); 
	   }
	}

	private Object extractKey(Message<String> message) {
		return (null == entityKeyExtractor) ? message.getPayload() : entityKeyExtractor.getKey(message);
	}

	private int delay() {
	    int delayFor = 0;
		if (delay > 0){
			try {
				delayFor = random.nextInt(delay);
				Thread.sleep(delayFor);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		return delayFor;
	}

	public void setEntityLock(EntityLock entityLock) {
		this.entityLock = entityLock;
	}

	public void setDelay(int delay) {
		this.delay = delay;
	}
	
	public void setEntityKeyStrategy(EntityKeyExtractor<Message<?>,?> entityKeyStrategy){
		this.entityKeyExtractor = entityKeyStrategy;
	}
}
