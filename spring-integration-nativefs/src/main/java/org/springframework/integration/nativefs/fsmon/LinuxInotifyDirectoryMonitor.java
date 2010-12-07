package org.springframework.integration.nativefs.fsmon;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.log4j.Logger;


/**
 * Linux implementation of {@link DirectoryMonitor}
 *
 * @author Josh Long
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

	@Override
	public native void startMonitor(String path) ;

	@Override
	public void stopMonitor(String path) {
		// noop for now
	}

	@Override
	protected void onInit() {
		// noop for now 
	}
}
