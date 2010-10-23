package org.springframework.integration.checkpoint;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.SmartLifecycle;
import org.springframework.integration.store.MessageGroupStore;
import org.springframework.integration.store.MessageGroupStoreReaper;

/**
 * An implementation of {@link MessageGroupStoreReaper} that implements {@link SmartLifecycle}
 * 
 * @author David Turanski
 *
 */
public class SmartMessageGroupStoreReaper extends MessageGroupStoreReaper implements SmartLifecycle {
	private boolean running;
	private static Log logger = LogFactory.getLog(SmartMessageGroupStoreReaper.class);
	private int phase = Integer.MIN_VALUE;
	
	public SmartMessageGroupStoreReaper(){
		super();
	}
	
	public SmartMessageGroupStoreReaper(MessageGroupStore messageGroupStore) {
		super(messageGroupStore);
	}
	
	@Override
	public void start() {
		running = true;
		if (logger.isInfoEnabled()){
		  logger.info("started " + this);
		}
	}
	
	@Override
	public void stop() {
		try {
			destroy();
			if (logger.isInfoEnabled()){
				  logger.info("started " + this);
			};
		} catch (Exception e) {
			logger.error("failed to stop bean",e);
		} finally {
			running = false;
		}
	}
	
	@Override
	public boolean isRunning() {
		return running;
	}
	
	@Override
	public int getPhase() {
		return phase;
	}
	
	/**
	 *  
	 * @param phase the start/stop order
	 */
	public void setPhase(int phase){
		this.phase = phase;
	}

	public boolean isAutoStartup() {
		return true;
	}

	public void stop(Runnable callback) {
		stop();
		callback.run();
	}
}
