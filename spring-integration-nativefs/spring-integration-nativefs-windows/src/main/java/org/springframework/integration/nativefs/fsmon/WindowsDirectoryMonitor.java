package org.springframework.integration.nativefs.fsmon;

/**
 * implementation of Windows {@link org.springframework.integration.nativefs.fsmon.DirectoryMonitor}
 * <p/>
 * todo
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

	@Override
	public boolean isNativeDependencyRequired() {
		return true;
	}
}
