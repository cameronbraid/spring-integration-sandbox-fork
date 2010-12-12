package org.springframework.integration.nativefs;

import org.springframework.integration.nativefs.fsmon.DirectoryMonitor;
import org.springframework.integration.nativefs.fsmon.LinuxInotifyDirectoryMonitor;

public class LinuxInotifyDirectoryMonitorClient extends AbstractDirectoryMonitorClient {

	public static void main(String[] args) throws Throwable {
		LinuxInotifyDirectoryMonitorClient c = new LinuxInotifyDirectoryMonitorClient();
		c.start();
	}

	@Override
	protected DirectoryMonitor initDirectoryMonitor() throws Throwable {
		final LinuxInotifyDirectoryMonitor monitor = new LinuxInotifyDirectoryMonitor();
		monitor.setExecutor(this.executor);
		monitor.afterPropertiesSet();
		return monitor;
	}
}
