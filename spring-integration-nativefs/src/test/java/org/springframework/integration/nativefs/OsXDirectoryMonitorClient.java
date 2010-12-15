package org.springframework.integration.nativefs;

import org.springframework.integration.nativefs.fsmon.DirectoryMonitor;
import org.springframework.integration.nativefs.fsmon.OsXDirectoryMonitor;


/**
 * client to monitor files using OSX fileeventstream
 *
 * @author  Josh Long
 * @since 2.1
 */
public class OsXDirectoryMonitorClient extends AbstractDirectoryMonitorClient {

   public static void main(String[] args) throws Throwable {
		OsXDirectoryMonitorClient c = new OsXDirectoryMonitorClient();
		c.start();
	}

	@Override
	protected DirectoryMonitor initDirectoryMonitor() throws Throwable {
		final OsXDirectoryMonitor monitor = new OsXDirectoryMonitor();
		monitor.setExecutor(this.executor);
		monitor.afterPropertiesSet();
		return monitor;
	}
}
