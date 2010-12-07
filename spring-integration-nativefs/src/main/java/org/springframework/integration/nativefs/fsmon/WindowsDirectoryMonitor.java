package org.springframework.integration.nativefs.fsmon;

import java.io.File;

/**
 * implementation of Windows {@link DirectoryMonitor}
 *
 * @author Josh Long
 */
public class WindowsDirectoryMonitor extends AbstractDirectoryMonitor {
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
