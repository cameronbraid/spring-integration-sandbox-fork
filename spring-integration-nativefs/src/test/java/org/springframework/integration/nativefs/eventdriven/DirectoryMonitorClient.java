package org.springframework.integration.nativefs.eventdriven;

import org.apache.commons.lang.SystemUtils;
import org.springframework.integration.nativefs.fsmon.DirectoryMonitor;
import org.springframework.integration.nativefs.fsmon.LinuxInotifyDirectoryMonitor;

import java.io.File;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;


/**
 * Simple client that exercises the {@link org.springframework.integration.nativefs.fsmon.DirectoryMonitor} implementations.
 *
 * @author Josh Long
 * @since 1.0
 */
public class DirectoryMonitorClient {
	public static void main(String[] args) throws Throwable {

		//String usrHome = System.getPropert

		File[] files = {
				new File(new File(SystemUtils.getUserHome(), "Desktop"), "test2"),
				new File(new File(SystemUtils.getUserHome(), "Desktop"), "test1")};

		for (File f : files)
			if (!f.exists())
				f.mkdirs();

		Executor ex = Executors.newFixedThreadPool(10);

		final LinuxInotifyDirectoryMonitor monitor = new LinuxInotifyDirectoryMonitor();
		monitor.setExecutor(ex);
		monitor.afterPropertiesSet();

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
}
