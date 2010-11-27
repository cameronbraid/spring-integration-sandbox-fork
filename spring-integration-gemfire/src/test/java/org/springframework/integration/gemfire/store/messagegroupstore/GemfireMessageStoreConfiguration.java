package org.springframework.integration.gemfire.store.messagegroupstore;

import com.gemstone.gemfire.cache.Region;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.Message;
import org.springframework.integration.aggregator.CorrelationStrategy;
import org.springframework.integration.aggregator.HeaderAttributeCorrelationStrategy;
import org.springframework.integration.aggregator.ReleaseStrategy;
import org.springframework.integration.aggregator.SequenceSizeReleaseStrategy;
import org.springframework.integration.gemfire.store.GemfireMessageGroupStore;
import org.springframework.integration.gemfire.store.KeyValueMessageGroup;
import org.springframework.integration.gemfire.store.KeyValueMessageGroupStore;

import java.util.Map;


/**
 * Our aggregator needs a {@link org.springframework.integration.gemfire.store.KeyValueMessageGroupStore}
 * - this handles configuration of the ancialliary objects.
 *
 * @author Josh Long
 */
@Configuration
@SuppressWarnings({"unused"})
public class GemfireMessageStoreConfiguration {

	@Value("${correlation-header}")
	private String correlationHeader;

	@Value("#{unmarkedRegion}")
	private Map<String, Message<?>> unmarked;

	@Value("#{markedRegion}")
	private Map<String, Message<?>> marked;

	@Value("#{messageGroupRegion}")
	private Map<Object, KeyValueMessageGroup> messageGroupRegion;

	@Bean
	public ReleaseStrategy releaseStrategy() {
		return new SequenceSizeReleaseStrategy(false);
	}

	@Bean
	public CorrelationStrategy correlationStrategy() {
		return new HeaderAttributeCorrelationStrategy(this.correlationHeader);
	}

	@Bean
	public KeyValueMessageGroupStore gemfireMessageGroupStore() {
		return new KeyValueMessageGroupStore(this.messageGroupRegion, this.marked , this.unmarked );
	}
}
