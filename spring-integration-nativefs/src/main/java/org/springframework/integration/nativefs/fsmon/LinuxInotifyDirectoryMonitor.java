package org.springframework.integration.nativefs.fsmon;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.log4j.Logger;


/**
 * Linux implementation of {@link DirectoryMonitor} that uses inotify (a Kernel facility) to handle directory monitoring.
 *
 * Inotify provides the easiest support and maps rather nicely to the contract set forth by this class and {@link AbstractDirectoryMonitor}
 *
 * @author Josh Long
 * @see AbstractDirectoryMonitor
 * @see DirectoryMonitor
 * @since 1.0
 */
public class LinuxInotifyDirectoryMonitor extends AbstractDirectoryMonitor {

	private static Logger logger = Logger.getLogger(LinuxInotifyDirectoryMonitor.class);

	static {
		try {
			System.loadLibrary("sifsmon");
		} catch (Throwable t) {
			logger.error("Received exception " + ExceptionUtils.getFullStackTrace(t) + " when trying to load the native library sifsmon");
		}
	}

    /**
     * Implementation from super class is processed directly by native code.
     *
     * @param path the path to be monitored
     */
	@Override
	public native void startMonitor(String path) ;

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
