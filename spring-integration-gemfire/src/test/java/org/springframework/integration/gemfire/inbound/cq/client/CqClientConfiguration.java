package org.springframework.integration.gemfire.inbound.cq.client;

import com.gemstone.gemfire.cache.Region;
import com.gemstone.gemfire.cache.client.*;
import com.gemstone.gemfire.cache.query.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.core.style.ToStringCreator;
import org.springframework.integration.MessageChannel;
import org.springframework.integration.gemfire.inbound.ContinuousQueryMessageProducer;
import org.springframework.integration.gemfire.inbound.cq.CqServiceActivator;


@Configuration
public class CqClientConfiguration {

//	private volatile Pool pool;
	//  private volatile ClientCache cache;
//	private volatile Region<?,?> region;
//	private volatile CqQuery cqQuery ;

	@Value("${region-name}")
	private String regionName;

	@Value("${host}")
	private String host;

	@Value("${region-query}")
	private String query;

	@Value("${port}")
	private int port;

	@Value("#{cqIn}")
	private MessageChannel messageChannel;


	@Bean
	public CqServiceActivator cqServiceActivator() {
		return new CqServiceActivator();
	}

	@Bean
	public ClientCache clientCache() throws Throwable {
		return buildCache();
	}

	@Bean
	public Region<?, ?> clientRegion() throws Throwable {
		ClientRegionFactory<?, ?> clientRegionFactory =
				clientCache().createClientRegionFactory(ClientRegionShortcut.PROXY);
		return clientRegionFactory.create(this.regionName);
	}

	@Bean
	public Pool pool() throws Throwable {
		return this.buildPool(this.host, this.port);
	}

	@Bean
	public ContinuousQueryMessageProducer continuousQueryMessageProducer() throws Throwable {
		ContinuousQueryMessageProducer continuousQueryMessageProducer
				= new ContinuousQueryMessageProducer( this.clientRegion() , this.pool(), this.query);
		continuousQueryMessageProducer.setDurable(true);
		continuousQueryMessageProducer.setOutputChannel(this.messageChannel);
		continuousQueryMessageProducer.setQueryName("pplQuery");
		return continuousQueryMessageProducer;
	}

/*	protected CqQuery registerContinuousQuery(QueryService queryService, String name, String query, boolean durable, CqListener cqListener) throws Throwable {
		CqAttributesFactory cqAttributesFactory = new CqAttributesFactory();
		cqAttributesFactory.addCqListener(cqListener);
		CqAttributes attrs = cqAttributesFactory.create();
		CqQuery cqQuery = queryService.newCq(name, query, attrs, durable);
		cqQuery.execute();
		return cqQuery;
	}*/

	protected ClientCache buildCache() throws Throwable {
		return new ClientCacheFactory().create();
	}

	protected Pool buildPool(String host, int port) throws Throwable {
		Pool pool = PoolManager.createFactory()
				.addServer(host, port)
				.setSubscriptionEnabled(true)
				.create(host + "Pool");
		return pool;
	}


	public static void main(String[] args) throws Exception {
		ClassPathXmlApplicationContext classPathXmlApplicationContext = new ClassPathXmlApplicationContext(
				"org/springframework/integration/gemfire/inbound/cq/CqClient-context.xml");

		while (true)
			Thread.sleep(1000 * 10);
	}

	/**
	 * the continuous query listener attached to the
	 */
	/*class MyContinuousQueryListener implements CqListener {
		public void onEvent(CqEvent cqEvent) {
			System.out.println("Received event: " +
					new ToStringCreator(cqEvent));
		}

		public void onError(CqEvent cqEvent) {
		}

		public void close() {
		}
	}*/
}
