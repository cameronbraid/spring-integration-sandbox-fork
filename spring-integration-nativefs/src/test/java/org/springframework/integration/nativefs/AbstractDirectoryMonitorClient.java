package org.springframework.integration.nativefs;

import org.apache.commons.lang.SystemUtils;
import org.springframework.integration.nativefs.fsmon.DirectoryMonitor;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * implementations can use this to exercise the code.
 *
 */
public abstract class AbstractDirectoryMonitorClient {

	protected Executor executor;

	protected Collection<File> getFiles() {
		return Arrays.asList(
				new File(new File(SystemUtils.getUserHome(), "Desktop"), "test2"),
				new File(new File(SystemUtils.getUserHome(), "Desktop"), "test1"));
	}

	/**
	 * each client can instantiate the appropriate one and exercise it.
 	 */
	protected abstract DirectoryMonitor initDirectoryMonitor() throws Throwable ;

	/**
	 * logic to setup the clients and demonstrate their use
	 *
	 */
	protected void setup() throws Throwable {

		Collection<File> files = this.getFiles();

		for (File f : files)
			if (!f.exists())
				f.mkdirs();

		this.executor = Executors.newFixedThreadPool(10);

		final DirectoryMonitor monitor = initDirectoryMonitor();

		final DirectoryMonitor.FileAddedListener fileAddedListener = new DirectoryMonitor.FileAddedListener() {
			@Override
			public void fileAdded(File dir, String fn) {
				System.out.println("A new file in " + dir.getAbsolutePath() + " called " + fn + " has been noticed");
			}
		};

		for (File f : files) {
			monitor.monitor(f, fileAddedListener);
		}

	}

	public void start() throws Throwable {
		setup();
	}
}
