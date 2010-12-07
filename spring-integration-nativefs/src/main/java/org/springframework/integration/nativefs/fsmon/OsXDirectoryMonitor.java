package org.springframework.integration.nativefs.fsmon;

import java.io.File;

/***
 *
 * implementation of the {@link org.springframework.integration.nativefs.fsmon.DirectoryMonitor}
 * interface that supports OS X
 *
 * @author Josh Long
 */
public class OsXDirectoryMonitor extends AbstractDirectoryMonitor {
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
