package org.springframework.integration.gemfire.store;

import com.gemstone.gemfire.cache.Region;
import org.springframework.integration.Message;

/**
 * this class provides Gemfire specific support as a backing key-value based {@link org.springframework.integration.store.MessageGroupStore},
 *
 * Currently, this support is limited to explicitly depending on Gemfire {@link com.gemstone.gemfire.cache.Region}s, but
 * might conceptually also support optimized key traversal (using a {@link com.gemstone.gemfire.cache.query.Query}, for example)
 *
 * @see {@link org.springframework.integration.gemfire.store.KeyValueMessageGroupStore}
 * @since 2.1
 * @author Josh Long
 */
@SuppressWarnings("unused")
public class GemfireMessageGroupStore extends KeyValueMessageGroupStore {

	public GemfireMessageGroupStore(
			Region<Object, KeyValueMessageGroup> groupIdToMessageGroup,
			Region<String, Message<?>> marked,
			Region<String, Message<?>> unmarked ) {
		super(groupIdToMessageGroup, marked, unmarked);
	}
}
