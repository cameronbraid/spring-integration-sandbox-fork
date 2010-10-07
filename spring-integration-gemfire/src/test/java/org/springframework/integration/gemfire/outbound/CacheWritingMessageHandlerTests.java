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

package org.springframework.integration.gemfire.outbound;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import org.springframework.data.gemfire.CacheFactoryBean;
import org.springframework.data.gemfire.RegionFactoryBean;
import org.springframework.integration.Message;
import org.springframework.integration.support.MessageBuilder;

import com.gemstone.gemfire.cache.Cache;
import com.gemstone.gemfire.cache.Region;

/**
 * @author Mark Fisher
 */
public class CacheWritingMessageHandlerTests {

	@Test
	public void mapPayloadWritesToCache() throws Exception {
		CacheFactoryBean cacheFactoryBean = new CacheFactoryBean();
		cacheFactoryBean.afterPropertiesSet();
		Cache cache = cacheFactoryBean.getObject();
		RegionFactoryBean<String, String> regionFactoryBean = new RegionFactoryBean<String, String>();
		regionFactoryBean.setName("test.mapPayloadWritesToCache");
		regionFactoryBean.setCache(cache);
		regionFactoryBean.afterPropertiesSet();
		Region<String, String> region = regionFactoryBean.getObject();
		assertEquals(0, region.size());
		CacheWritingMessageHandler handler = new CacheWritingMessageHandler(region);
		Map<String, String> map = new HashMap<String, String>();
		map.put("foo", "bar");
		Message<?> message = MessageBuilder.withPayload(map).build();
		handler.handleMessage(message);
		assertEquals(1, region.size());
		assertEquals("bar", region.get("foo"));
	}

}
