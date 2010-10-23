package org.springframework.integration.checkpoint;

import org.springframework.integration.Message;
import org.springframework.integration.MessageChannel;
import org.springframework.integration.channel.interceptor.ChannelInterceptorAdapter;

/**
 * This is used to inject some sleep time between in each message channel to observe the 
 * {@ link CheckpointCollector} aggregating messages at regular intervals during testing
 * 
 * @author David Turanski
 *
 */
public class RandomDelayInterceptor extends ChannelInterceptorAdapter {
	private long interval;

	/**
	 * @param interval the maximum sleep time in ms
	 */
	public void setInterval(long interval) {
		this.interval = interval;
	}

	@Override 
	public Message<?> preSend(Message<?> message, MessageChannel channel) {
		if (interval > 0){
			try {
				Thread.sleep((long) Math.rint(interval));
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return message;
	}


}
