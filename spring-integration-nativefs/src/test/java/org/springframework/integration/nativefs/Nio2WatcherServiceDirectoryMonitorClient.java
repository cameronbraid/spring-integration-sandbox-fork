package org.springframework.integration.nativefs;

import org.springframework.integration.nativefs.fsmon.DirectoryMonitor;
//import org.springframework.integration.nativefs.fsmon.Nio2WatchServiceDirectoryMonitor;

/**
 * Simple implementation of {@link AbstractDirectoryMonitorClient}
 *
 * @author Josh Long
 */
public class Nio2WatcherServiceDirectoryMonitorClient extends AbstractDirectoryMonitorClient {
	/*
	@Override
	protected DirectoryMonitor initDirectoryMonitor() throws Throwable {
		Nio2WatchServiceDirectoryMonitor directoryMonitor = new Nio2WatchServiceDirectoryMonitor();
		directoryMonitor.setExecutor(this.executor);
		directoryMonitor.afterPropertiesSet();

		return directoryMonitor;
	}

	public static void main(String[] a) throws Throwable {
		Nio2WatcherServiceDirectoryMonitorClient watcherServiceDirectoryMonitorClient =
                new Nio2WatcherServiceDirectoryMonitorClient();

		watcherServiceDirectoryMonitorClient.start();
	}
	 */

    @java.lang.Override
    protected DirectoryMonitor initDirectoryMonitor() throws Throwable {
        return null;
    }
}
