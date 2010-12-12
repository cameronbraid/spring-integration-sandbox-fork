package org.springframework.integration.nativefs;

import org.springframework.integration.nativefs.fsmon.DirectoryMonitor;
import org.springframework.integration.nativefs.fsmon.Nio2WatchServiceDirectoryMonitor;

/**
 * @author Josh Long
 *         <p/>
 *         Simple implementation of {@link AbstractDirectoryMonitorClient}
 */
public class Nio2WatcherServiceDirectoryMonitorClient extends AbstractDirectoryMonitorClient {
	@Override
	protected DirectoryMonitor initDirectoryMonitor() throws Throwable {
		Nio2WatchServiceDirectoryMonitor m = new Nio2WatchServiceDirectoryMonitor();
		m.setExecutor(this.executor);
		m.afterPropertiesSet();
		return m;
	}

	public static void main(String[] a) throws Throwable {
		Nio2WatcherServiceDirectoryMonitorClient c = new Nio2WatcherServiceDirectoryMonitorClient();
		c.start();
	}
}
