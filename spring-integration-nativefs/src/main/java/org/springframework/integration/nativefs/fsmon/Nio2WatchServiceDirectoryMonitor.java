package org.springframework.integration.nativefs.fsmon;

/**
 * this will be an implementation that delegates to a a JDK 7 NIO.2 based WatchService if available
 *
 * @author Josh Long
 */
public class Nio2WatchServiceDirectoryMonitor extends AbstractDirectoryMonitor {

	@Override
	protected void startMonitor(String path) {
	}

	@Override
	protected void stopMonitor(String path) {
	}

	@Override
	protected void onInit() {
	}
}
