package org.springframework.integration.gemfire.inbound.cq.server;

import com.gemstone.gemfire.cache.Cache;
import com.gemstone.gemfire.cache.Region;
import com.gemstone.gemfire.cache.server.CacheServer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.data.gemfire.GemfireTemplate;


@Configuration
public class CqServerConfiguration {


	@Value("#{c}")
	private Cache cache;

	@Value("#{r}")
	private Region<String, ?> region;


	@Value("${region-name}")
	private String regionName;

	@Value("${host}")
	private String host;

	@Value("${port}")
	private int port;

	@Bean
	public GemfireTemplate gemfireTemplate() {
		return new GemfireTemplate(this.region);
	}

	@Bean
	public CacheServer cacheServer() throws Throwable {
		CacheServer cacheServer = this.cache.addCacheServer();
		cacheServer.setBindAddress(this.host);
		cacheServer.setPort(this.port);
		cacheServer.start();
		return cacheServer;
	}

	public static void main(String[] args) throws Exception {
		ClassPathXmlApplicationContext applicationContext = new ClassPathXmlApplicationContext(
				"org/springframework/integration/gemfire/inbound/cq/CqServer-context.xml");

		applicationContext.registerShutdownHook();
		applicationContext.start();

		GemfireTemplate gemfireTemplate = applicationContext.getBean(GemfireTemplate.class);

		String letters = "abcdefghijk";

		while (true) {

			Thread.sleep(1000 * 10);

			for (char c : letters.toCharArray())
				gemfireTemplate.put("" + c, "value-" + c);

		}
	}

}


