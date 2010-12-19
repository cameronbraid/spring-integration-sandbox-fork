package org.springframework.integration.nativefs.fsmon;

import java.io.File;

/**
 * interface which describes a directory startMonitor. the
 * {@link DirectoryMonitor#monitor(java.io.File, org.springframework.integration.nativefs.fsmon.DirectoryMonitor.FileAddedListener)}  method must
 * be called with a proper directory and a {@link DirectoryMonitor.FileAddedListener} instance
 *
 * @author Josh Long
 * @see AbstractDirectoryMonitor
 * @see Nio2WatchServiceDirectoryMonitor
 * @see LinuxInotifyDirectoryMonitor
 * @see OsXDirectoryMonitor
 * @see WindowsDirectoryMonitor
 */
public interface DirectoryMonitor {

	/**
	 * this instance will create a startMonitor on the directory (the <code>file</code> param must not be null) and dispatch to
	 * the {@link DirectoryMonitor.FileAddedListener}
	 * implementation whenever a file is observed in the directory.
	 *
     * @param file the directory to monitor. The parameter must be a {@link File} reference, and {@link java.io.File#isDirectory()} must return true
     *
     * @param fal the {@link DirectoryMonitor.FileAddedListener} is the hook for clients to call when a directory perceives a new file.
     *
	 */
	void monitor(File file, FileAddedListener fal);

	/**
	 * {@link DirectoryMonitor.FileAddedListener}
	 * instances are registered with the {@link DirectoryMonitor}
	 * instance and, whenever a file that matches is observed, it calls #startMonitor(FileAddedListener)
	 */
	interface FileAddedListener {

		void fileAdded(File dir, String fn);
	}
}
