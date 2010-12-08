package org.springframework.integration.nativefs;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.MessageChannel;

import javax.annotation.PostConstruct;
import java.io.File;


@Configuration
public class DirectoryMonitorInboundFileEndpointConfiguration {

	@Value("#{fileChannel}")
	private MessageChannel messageChannel;

	@Value("${test-folder}")
	private String testFolder;

	private File file;

	@PostConstruct
	public void setup() throws Throwable {
		file = new File(testFolder);
	}


	@Bean
	public DirectoryMonitorInboundFileEndpoint directoryMonitorInboundFileEndpoint() {
		DirectoryMonitorInboundFileEndpoint en = new DirectoryMonitorInboundFileEndpoint();
		en.setOutputChannel(this.messageChannel);
		en.setDirectoryToMonitor(file);

		return en;
	}
}
