/*
 * Copyright 2002-2010 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.gemfire.store;

import com.gemstone.gemfire.cache.Region;
import org.springframework.integration.Message;

/**
 * this class provides Gemfire specific support as a backing key-value based {@link org.springframework.integration.store.MessageGroupStore},
 *
 * Currently, this support is limited to explicitly depending on Gemfire {@link com.gemstone.gemfire.cache.Region}s, but
 * might conceptually also support optimized key traversal (using a {@link com.gemstone.gemfire.cache.query.Query}, for example)
 *
 * @since 2.1
 * @author Josh Long
 * @see {@link org.springframework.integration.gemfire.store.KeyValueMessageGroupStore}
 */
public class GemfireMessageGroupStore extends KeyValueMessageGroupStore {

	public GemfireMessageGroupStore(
			Region<Object, KeyValueMessageGroup> groupIdToMessageGroup,
			Region<String, Message<?>> marked,
			Region<String, Message<?>> unmarked ) {
		super(groupIdToMessageGroup, marked, unmarked);
	}

}
