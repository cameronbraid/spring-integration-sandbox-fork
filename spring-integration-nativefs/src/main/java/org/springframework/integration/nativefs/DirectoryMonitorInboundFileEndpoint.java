package org.springframework.integration.nativefs;

import org.apache.log4j.Logger;
import org.springframework.integration.Message;
import org.springframework.integration.endpoint.MessageProducerSupport;
import org.springframework.integration.file.FileHeaders;
import org.springframework.integration.nativefs.fsmon.DirectoryMonitor;
import org.springframework.integration.nativefs.fsmon.LinuxInotifyDirectoryMonitor;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

import java.io.File;
import java.util.concurrent.Executor;


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

	private static Logger logger = Logger.getLogger( DirectoryMonitorInboundFileEndpoint.class);

	private File directoryToMonitor;
	private DirectoryMonitor monitor;
	private Executor executor;

	/**
	 * is JDK 7 NIO.2 support for WatchServices available?
	 *
	 * @return
	 */
	private boolean hasJdk7WatchService() {
		String watchService = "java.nio.file.WatchService";

		boolean hasWatchServiceClazz = true;

		try {
			ClassUtils.forName(watchService, ClassUtils.getDefaultClassLoader());
		} catch (Exception e) {
			hasWatchServiceClazz = false;
		}

		return hasWatchServiceClazz;
	}

	/**
	 * tells whether or not the host operating system is Linux and supports dispatching using the inotify
	 * kernel facility. inotify has been available in Linux kernel since 2.6.13
	 */
	private boolean supportsLinuxInotify() {
		boolean inotifySupported = false;

		String os = System.getProperty("os.name");
		String version = System.getProperty("os.version");
		String[] versionPieces = version.split("\\.");

		if (os.toLowerCase().indexOf("linux") != -1) {
			if (versionPieces.length >= 2) {
				int majorVersion = Integer.parseInt(versionPieces[0]);
				int minorVersion = Integer.parseInt(versionPieces[1]);
				int microVersion = 0;

				if (versionPieces.length > 2) {
					String[] microVersionPieces = versionPieces[2].split("-");

					if (microVersionPieces.length > 0) {
						microVersion = Integer.parseInt(microVersionPieces[0]);
					}
				}

				inotifySupported = ((majorVersion > 2) ||
						((majorVersion == 2) && (minorVersion > 6)) ||
						((majorVersion == 2) && (minorVersion == 6) &&
								(microVersion >= 13)));
			}
		}

		return inotifySupported;
	}

	@Override
	protected void onInit() {
		try {
			DirectoryMonitor resultingMonitor = null;

			if (supportsLinuxInotify()) {
				LinuxInotifyDirectoryMonitor linuxInotifyDirectoryMonitor = new LinuxInotifyDirectoryMonitor();

				if (executor != null) {
					linuxInotifyDirectoryMonitor.setExecutor(executor);
				}

				linuxInotifyDirectoryMonitor.afterPropertiesSet();
				resultingMonitor = linuxInotifyDirectoryMonitor;
			}

			this.monitor = resultingMonitor;
		} catch (Exception e) {
			throw new RuntimeException("Exception thrown when trying to setup " +
					DirectoryMonitorInboundFileEndpoint.class, e);
		}
	}

	@Override
	protected void doStart() {
		Assert.notNull(monitor, "the startMonitor can't be null");

		MessageProducingFileAddedListener messageProducingFileAddedListener = new MessageProducingFileAddedListener();
		monitor.monitor(directoryToMonitor, messageProducingFileAddedListener);
	}

	@Override
	protected void doStop() {
	}

	public void setDirectoryToMonitor(File directoryToMonitor) {
		this.directoryToMonitor = directoryToMonitor;
	}

	class MessageProducingFileAddedListener implements DirectoryMonitor.FileAddedListener {
		@Override
		public void fileAdded(File dir, String fn) {
			File fi = new File(dir, fn);
			Message<File> msg = MessageBuilder.withPayload(fi)
					.setHeader(FileHeaders.FILENAME,
							fi.getPath()).build();
			sendMessage(msg);
		}
	}
}
