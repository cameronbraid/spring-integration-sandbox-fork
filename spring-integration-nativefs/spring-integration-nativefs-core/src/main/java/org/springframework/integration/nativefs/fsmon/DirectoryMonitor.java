package org.springframework.integration.nativefs.fsmon;

import java.io.File;

/**
 * interface which describes a directory startMonitor. the
 * {@link org.springframework.integration.nativefs.fsmon.DirectoryMonitor#monitor(java.io.File, org.springframework.integration.nativefs.fsmon.DirectoryMonitor.FileAddedListener)}  method must
 * be called with a proper directory and a {@link org.springframework.integration.nativefs.fsmon.DirectoryMonitor.FileAddedListener} instance
 *
 * @author Josh Long
 * @see AbstractDirectoryMonitor
 */
public interface DirectoryMonitor {

	/**
	 * this instance will create a startMonitor on the directory (the <code>file</code> param must not be null) and dispatch to
	 * the {@link org.springframework.integration.nativefs.fsmon.DirectoryMonitor.FileAddedListener}
	 * implementation whenever a file is observed in the directory.
	 *
	 * @param file the directory to monitor. The parameter must be a {@link java.io.File} reference, and {@link java.io.File#isDirectory()} must return true
	 * @param fal	the {@link org.springframework.integration.nativefs.fsmon.DirectoryMonitor.FileAddedListener} is the hook for clients to call when a directory perceives a new file.
	 */
	void monitor(File file, FileAddedListener fal);

	/**
	 * {@link org.springframework.integration.nativefs.fsmon.DirectoryMonitor.FileAddedListener}
	 * instances are registered with the {@link org.springframework.integration.nativefs.fsmon.DirectoryMonitor}
	 * instance and, whenever a file that matches is observed, it calls #startMonitor(FileAddedListener)
	 */
	interface FileAddedListener {

		void fileAdded(File dir, String fn);
	}
}
