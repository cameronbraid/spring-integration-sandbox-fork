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

package org.springframework.integration.gemfire.inbound;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.integration.endpoint.MessageProducerSupport;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.util.Assert;

import com.gemstone.gemfire.cache.CacheListener;
import com.gemstone.gemfire.cache.EntryEvent;
import com.gemstone.gemfire.cache.Region;
import com.gemstone.gemfire.cache.util.CacheListenerAdapter;

/**
 * @author Mark Fisher
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class CacheListeningMessageProducer extends MessageProducerSupport {

	private final Log logger = LogFactory.getLog(this.getClass());

	private final Region region;

	private final CacheListener<?, ?> listener;

	private volatile Set<EventType> supportedEventTypes =
			new HashSet<EventType>(Arrays.asList(EventType.CREATED, EventType.UPDATED));

	private volatile Expression payloadExpression;

	private final SpelExpressionParser parser = new SpelExpressionParser();


	public CacheListeningMessageProducer(Region<?, ?> region) {
		Assert.notNull(region, "region must not be null");
		this.region = region;
		this.listener = new MessageProducingCacheListener();
	}


	public void setSupportedEventTypes(EventType... eventTypes) {
		Assert.notEmpty(eventTypes, "eventTypes must not be empty");
		this.supportedEventTypes = new HashSet<EventType>(Arrays.asList(eventTypes));
	}

	public void setPayloadExpression(String payloadExpression) {
		if (payloadExpression == null) {
			this.payloadExpression = null;
		}
		else {
			this.payloadExpression = this.parser.parseExpression(payloadExpression);
		}
	}

	@Override
	protected void doStart() {
		if (logger.isInfoEnabled()) {
			logger.info("adding MessageProducingCacheListener to GemFire Region '" + this.region.getName() + "'");
		}
		this.region.getAttributesMutator().addCacheListener(this.listener);
	}

	@Override
	protected void doStop() {
		if (logger.isInfoEnabled()) {
			logger.info("removing MessageProducingCacheListener from GemFire Region '" + this.region.getName() + "'");
		}
		this.region.getAttributesMutator().removeCacheListener(this.listener);
	}


	private class MessageProducingCacheListener extends CacheListenerAdapter {

		@Override
		public void afterCreate(EntryEvent event) {
			if (supportedEventTypes.contains(EventType.CREATED)) {
				this.processEvent(event);
			}
		}

		@Override
		public void afterUpdate(EntryEvent event) {
			if (supportedEventTypes.contains(EventType.UPDATED)) {
				this.processEvent(event);
			}
		}

		@Override
		public void afterInvalidate(EntryEvent event) {
			if (supportedEventTypes.contains(EventType.INVALIDATED)) {
				this.processEvent(event);
			}
		}

		@Override
		public void afterDestroy(EntryEvent event) {
			if (supportedEventTypes.contains(EventType.DESTROYED)) {
				this.processEvent(event);
			}
		}

		private void processEvent(EntryEvent event) {
			if (payloadExpression != null) {
				Object evaluationResult = payloadExpression.getValue(event);
				this.publish(evaluationResult);
			}
			else {
				this.publish(event);
			}
		}

		private void publish(Object payload) {
			sendMessage(MessageBuilder.withPayload(payload).build());
		}
	}

}
