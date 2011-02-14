package org.springframework.integration.nativefs.fsmon.linux;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.integration.nativefs.fsmon.AbstractDirectoryMonitor;


/**
 * Linux implementation of {@link org.springframework.integration.nativefs.fsmon.DirectoryMonitor} that uses inotify (a Kernel facility) to handle directory monitoring.
 * <p/>
 * Inotify provides the easiest support and maps rather nicely to the contract set forth by this class and {@link org.springframework.integration.nativefs.fsmon.AbstractDirectoryMonitor}
 *
 * @author Josh Long
 * @see org.springframework.integration.nativefs.fsmon.AbstractDirectoryMonitor
 * @see org.springframework.integration.nativefs.fsmon.DirectoryMonitor
 * @since 1.0
 */
public class LinuxInotifyDirectoryMonitor extends AbstractDirectoryMonitor {

	private static Log log = LogFactory.getLog(LinuxInotifyDirectoryMonitor.class);

	@Override
	public boolean isNativeDependencyRequired() {
		return true;
	}

	static {
		try {

			if (log.isDebugEnabled())
				log.debug("the current library path is " + System.getProperty("java.library.path"));

			System.loadLibrary("sifsmon");

		} catch (Throwable t) {
			log.error("Received exception " + ExceptionUtils.getFullStackTrace(t) + " when trying to load the native library sifsmon");
		}
	}

	/**
	 * Implementation from super class is processed directly by native code.
	 *
	 * @param path the path to be monitored
	 */
	@Override
	public native void startMonitor(String path);

	/**
	 * Implementation doesn't currently support dismantling watches. This would invariably be a native code invocation, to be effective.
	 *
	 * @param path the path for whom any watches should de dismantled
	 */
	@Override
	public void stopMonitor(String path) {
		// noop for now
	}

	@Override
	protected void onInit() {
		// noop for now 
	}
}
