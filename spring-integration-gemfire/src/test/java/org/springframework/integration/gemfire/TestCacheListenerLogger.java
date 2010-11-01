package org.springframework.integration.gemfire;

import com.gemstone.gemfire.cache.EntryEvent;
import com.gemstone.gemfire.cache.util.CacheListenerAdapter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * (this is the CacheLogger class that ships in the Spring-Gemfire samples)
 * 
 * @author Costin Leau
 */
public class TestCacheListenerLogger extends CacheListenerAdapter<Object, Object> {

	private static final Log log = LogFactory.getLog(TestCacheListenerLogger.class);

	@Override
	public void afterCreate(EntryEvent<Object, Object> event) {
		log.info("Added " + messageLog(event) + " to the cache");
	}

	@Override
	public void afterDestroy(EntryEvent<Object, Object> event) {
		log.info("Removed " + messageLog(event) + " from the cache");
	}

	@Override
	public void afterUpdate(EntryEvent<Object, Object> event) {
		log.info("Updated " + messageLog(event) + " in the cache");
	}

	private String messageLog(EntryEvent<Object, Object> event) {
		Object key = event.getKey();
		Object value = event.getNewValue();

		if (event.getOperation().isUpdate()) {
			return "[" + key + "] from [" + event.getOldValue() + "] to [" + event.getNewValue() + "]";
		}
		return "[" + key + "=" + value + "]";
	}
}
