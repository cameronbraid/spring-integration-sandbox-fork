package org.springframework.integration.nativefs;

import org.springframework.integration.Message;
import org.springframework.integration.endpoint.MessageProducerSupport;
import org.springframework.integration.file.FileHeaders;
import org.springframework.integration.nativefs.fsmon.DirectoryMonitor;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.util.Assert;

import java.io.File;


/**
 * a Spring Integration adapter that delegates to an instance of a {@link org.springframework.integration.nativefs.fsmon.DirectoryMonitor} to
 * perceive new {@link java.io.File}s added to a monitored directory.
 * <p/>
 * This is a useful alternative to Spring Integration's prebuilt file adapters because it reacts instantly to the availability of new files.
 *
 * @author Josh Long
 * @since 1.0
 */
public class DirectoryMonitorInboundFileEndpoint extends MessageProducerSupport {

	private File directoryToMonitor;
	private DirectoryMonitor monitor;


	/**
	 * the monitor
	 *
	 * @param dm the {@link DirectoryMonitor} implementation.
	 * @see {@link DirectoryMonitorFactory} for a clean way of factorying the correct implementation appropriate to your operating system and JDK
	 */
	public void setMonitor(DirectoryMonitor dm) {
		this.monitor = dm;
	}

	@Override
	protected void onInit() {
		Assert.notNull(this.directoryToMonitor, "the directoryToMonitor can't be null");
		Assert.notNull(this.monitor, "the monitor implementation to use can't be null. See DirectoryMonitorFactory");

	}

	@Override
	protected void doStart() {

		MessageProducingFileAddedListener messageProducingFileAddedListener = new MessageProducingFileAddedListener();
		monitor.monitor(directoryToMonitor, messageProducingFileAddedListener);

	}

	@Override
	protected void doStop() {
	}

	public void setDirectoryToMonitor(File directoryToMonitor) {
		this.directoryToMonitor = directoryToMonitor;
	}

	/**
	 * {@link DirectoryMonitor.FileAddedListener} implementation that forwards events through a {@link org.springframework.integration.MessageChannel}
	 */
	class MessageProducingFileAddedListener implements DirectoryMonitor.FileAddedListener {
		@Override
		public void fileAdded(File dir, String fn) {
			File fi = new File(dir, fn);
			Message<File> msg = MessageBuilder.withPayload(fi).setHeader(FileHeaders.FILENAME, fi.getPath()).build();
			sendMessage(msg);
		}
	}
}
