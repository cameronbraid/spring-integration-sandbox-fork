package org.springframework.integration.nativefs.eventdriven;

import junit.framework.Assert;
import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.integration.Message;
import org.springframework.integration.MessageChannel;
import org.springframework.integration.MessagingException;
import org.springframework.integration.config.ConsumerEndpointFactoryBean;
import org.springframework.integration.core.MessageHandler;
import org.springframework.integration.endpoint.AbstractEndpoint;
import org.springframework.integration.nativefs.DirectoryMonitorInboundFileEndpoint;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.Writer;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.TimeUnit;


/**
 * Handles testing receiving messages from the filesystem.
 *
 * @author Josh Long
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class DirectoryMonitorInboundFileEndpointTest implements BeanFactoryAware {

	@Value("${test-folder}")
	private String folder;

	@Value("#{fileChannel}")
	private MessageChannel messageChannel;

	private File directoryToMonitor;
	private BeanFactory beanFactory;

	public AbstractEndpoint createConsumer(MessageChannel messageChannel,
																				 MessageHandler messageHandler) throws Throwable {
		ConsumerEndpointFactoryBean consumerEndpointFactoryBean = new ConsumerEndpointFactoryBean();
		consumerEndpointFactoryBean.setInputChannel(messageChannel);
		consumerEndpointFactoryBean.setBeanName(DirectoryMonitorInboundFileEndpoint.class.getName());
		consumerEndpointFactoryBean.setBeanFactory(beanFactory);
		consumerEndpointFactoryBean.setHandler(messageHandler);
		consumerEndpointFactoryBean.setBeanClassLoader(ClassLoader.getSystemClassLoader());
		consumerEndpointFactoryBean.afterPropertiesSet();

		AbstractEndpoint abstractEndpoint = consumerEndpointFactoryBean.getObject();
		abstractEndpoint.start();

		return abstractEndpoint;
	}

	@After
	public void stop() throws Throwable {
		if ((this.directoryToMonitor != null) &&
				this.directoryToMonitor.exists()) {
			try {
				this.directoryToMonitor.delete();
			} catch (Exception ex) {
				// noop
			}
		}
	}

	@Before
	public void start() throws Throwable {
		directoryToMonitor = new File(this.folder);

		if (directoryToMonitor.exists()) {
			directoryToMonitor.delete();
		}

		directoryToMonitor.mkdir();
	}

	@Test
	public void testReceivingFiles() throws Throwable {
		final Set<String> files = new ConcurrentSkipListSet<String>();

		createConsumer(this.messageChannel,
				new MessageHandler() {
					@Override
					public void handleMessage(Message<?> message)
							throws MessagingException {
						File file = (File) message.getPayload();
						String filePath = file.getPath();
						files.add(filePath);
					}
				});

		int cnt = 10;

		for (int i = 0; i < cnt; i++) {
			File out = new File(directoryToMonitor, i + ".txt");
			Writer w = new BufferedWriter(new FileWriter(out));
			IOUtils.write("test" + i, w);
			IOUtils.closeQuietly(w);
		}

		Thread.sleep(TimeUnit.SECONDS.toMillis(20));
		Assert.assertEquals(cnt, files.size());
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory)
			throws BeansException {
		this.beanFactory = beanFactory;
	}
}
