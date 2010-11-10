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
import org.springframework.integration.gemfire.store.GemfireMessageGroup;
import org.springframework.integration.gemfire.store.GemfireMessageGroupStore;

import java.util.UUID;

/**
 * configures the machinery for our aggregator
 *
 * @author Josh Long
 */
@Configuration
public class GemfireMessageStoreConfiguration {

	@Value( "${correlation-header}") private String correlationHeader;

	@Value("#{unmarkedRegion}") private volatile Region<String, Message<?>> unmarked ;

	@Value("#{markedRegion}") private volatile Region<String, Message<?>> marked ;

	@Value("#{messagesRegion}") private volatile Region<UUID,Message<?>> messageStoreRegion;

	@Value("#{messageGroupRegion}") private volatile Region<Object, GemfireMessageGroup> messageGroupRegion;

	@Bean
	public ReleaseStrategy releaseStrategy(){
		 return new SequenceSizeReleaseStrategy( false);
	}

	@Bean
	public CorrelationStrategy correlationStrategy (){
		return new HeaderAttributeCorrelationStrategy( this.correlationHeader );
	}

	@Bean
	public GemfireMessageGroupStore gemfireMessageGroupStore(){
		return new GemfireMessageGroupStore( this.messageStoreRegion, this.messageGroupRegion, this.marked, this.unmarked );
	}


}
