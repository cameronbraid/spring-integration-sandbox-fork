package org.springframework.integration.nativefs.fsmon.osx;

import java.io.File;
import java.io.FileWriter;
import java.io.Writer;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.Executors;


import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.SystemUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.integration.nativefs.fsmon.DirectoryMonitor;

public class OsXDirectoryMonitorTest {

	private static Log log = LogFactory.getLog( OsXDirectoryMonitorTest.class) ;

	private OsXDirectoryMonitor directoryMonitor;

	private File file = new File(SystemUtils.getJavaIoTmpDir(), "monitor");

	private TestFileAddedListener testFileAddedListener = new TestFileAddedListener();

	class TestFileAddedListener implements DirectoryMonitor.FileAddedListener {

		Set<File> filesRcved = new ConcurrentSkipListSet<File>();

		public int count() {
			return this.filesRcved.size();
		}

		@Override
		public void fileAdded(File dir, String fn) {
			this.filesRcved.add(new File(dir, fn));
		}
	}

	@Before
	public void start() throws Throwable {

		if (this.file.exists()) {

			for(File f: this.file.listFiles())
			f.delete();

			file.delete();

			if (file.exists())
				throw new RuntimeException("couldn't delete the file " + this.file.getAbsolutePath() + ", but we need to reset for the testcase.");
		}

		if (!file.mkdirs())
			throw new RuntimeException("couldn't create the directory " + this.file.getAbsolutePath());


		this.directoryMonitor = new OsXDirectoryMonitor  ();
		this.directoryMonitor.setExecutor(Executors.newSingleThreadExecutor());
		this.directoryMonitor.afterPropertiesSet();
		this.directoryMonitor.monitor(this.file, this.testFileAddedListener);

	}

	public void run() throws Throwable {
		start();
		testMonitoringDirectoryUnderLinux();
	}
	public static void main(String[] a ) throws Throwable {
		OsXDirectoryMonitorTest linuxDirectoryMonitorTest = new OsXDirectoryMonitorTest();
		linuxDirectoryMonitorTest.run();
	}

	@Test
	public void testMonitoringDirectoryUnderLinux() throws Throwable {

		String[] files = "a,b".split(",");
		// put a few files in the directory
		for (String x : files) {
			Writer writer = null;
			try {
				File n = new File(this.file, x + ".txt");
				writer = new FileWriter(n);
				IOUtils.write("content", writer);
			} finally {
				IOUtils.closeQuietly(writer);
			}
		}

		Assert.assertTrue("there are two " +
                "files in the directory now.",
                this.file.list().length == 2);

		long sleeping = 0;
		while (this.testFileAddedListener.count() < files.length && sleeping < (10 * 1000)) {
			int s = 1000;
			Thread.sleep(s); //wait a second
			sleeping += s;
		}

		Assert.assertEquals(files.length, this.testFileAddedListener.count());


	}
}
