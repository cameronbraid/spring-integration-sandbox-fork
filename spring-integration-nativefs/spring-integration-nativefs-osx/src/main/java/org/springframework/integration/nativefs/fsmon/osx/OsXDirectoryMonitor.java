package org.springframework.integration.nativefs.fsmon.osx;


import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.Logger;
import org.springframework.integration.nativefs.fsmon.AbstractDirectoryMonitor;
import org.springframework.util.Assert;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * implementation of the {@link org.springframework.integration.nativefs.fsmon.DirectoryMonitor}
 * interface that supports OS X.
 * <p/>
 * This works by registering a monitor using OSX's kernel facilities to receive callbacks of file system events.
 * <p/>
 * As OSX tells this class that <em>something</em> has changed in the underlying file system, we must manually
 * deliver deltas by scanning and calculating what's new.
 * <p/>
 * Meanwhile, in delivery threads (one for each directory monitored by this class) we deliver these new files and trigger listeners.
 *
 * @author Josh Long
 * @author 2.1
 */
public class OsXDirectoryMonitor extends AbstractDirectoryMonitor {


    private static Log log = LogFactory.getLog(OsXDirectoryMonitor.class);

	/**
	 * the mapping of files that we pick up each scanForNewFiles - each file maintains a queue of files to be delivered
	 */
	private volatile ConcurrentHashMap<File, LinkedBlockingQueue<File>> statefulMappingOfDirectoryContents =
			new ConcurrentHashMap<File, LinkedBlockingQueue<File>>();

	static {
		try {
            loadLibrary("libsifsmon.dylib");
        } catch (Throwable throwable){
            log.debug( "couldn't load the library.");
        }
	}

	@Override
	protected void startMonitor(String path) {

		// OSX specific adaptation because the kernel delivers events with file system paths that end with '/' so we need to be able to match that
		if (path != null && !path.endsWith("/"))
			path = path + "/";

		File f = new File(path);

		/**
		 * do one quick scan and get everything preloaded. NB: this doesn *not* enqueue files that were already in the directory
		 * when we first run this component. it simply ensures that we have a Queue setup inside #statefulMappingOfDirectoryContents
		 */
		scanForNewFiles(f);

		this.executor.execute(new DeliveryRunnable(this, f, this.statefulMappingOfDirectoryContents.get(f)));

		this.monitor(path);

	}

	@Override
	protected void stopMonitor(String path) {
		// nooop for now
	}

	/**
	 * this will delegate to the native .dynlib implementation to register a watch on a directory
	 *
	 * @param path the path to be monitored
	 */
	public native void monitor(String path);

	/**
	 * scans the directory and provides a delta for all files that are there now that weren't there in the last scanForNewFiles
	 *
	 * @param theDirectoryToScan the directory we should scanForNewFiles
	 *                           <p/>
	 *                           todo is it possible that we can deliver two events for the same {@link java.io.File} if the multithreading were to escalate? that is, can we enqueue a second event *as* the first is being delivered?
	 */
	private Set<File> scanForNewFiles(File theDirectoryToScan) {

		Collection<File> deltas = new ArrayList<File>();

		Assert.isTrue(theDirectoryToScan.exists() && theDirectoryToScan.isDirectory(), "the directory must still exist for this to work correctly");

		File[] dirListing = theDirectoryToScan.listFiles();

		if (dirListing == null)
			dirListing = new File[0];

		statefulMappingOfDirectoryContents.putIfAbsent(theDirectoryToScan, new LinkedBlockingQueue<File>());

		Queue<File> filesToDeliver = this.statefulMappingOfDirectoryContents.get(theDirectoryToScan);

		for (File ff : dirListing) {
			if (!filesToDeliver.contains(ff)) {
				deltas.add(ff);
			}
		}

		return new HashSet<File>(deltas);
	}

	/**
	 * this is the path that has changed. It DOES NOT tell us which files have changed, just that there was a change.
	 * We need to keep a stateful view of the path and do deltas.
	 * <p/>
	 * This code will be called <em>from</em> the java native code (JNI), so it needs to be very simple / quick.
	 *
	 * @param path the path that has received the changes
	 */
	@SuppressWarnings("unused")
	public synchronized void pathChanged(String path) {

		log.debug(String.format("the path %s has changed; rescanning....", path));

		File key = new File(path);
		Set<File> newFiles = this.scanForNewFiles(key);

		Queue<File> queueOfFiles = this.statefulMappingOfDirectoryContents.get(key);

		queueOfFiles.addAll(newFiles);
	}

	@Override
	protected void onInit() {
		// noop for now
	}

	@Override
	public boolean isNativeDependencyRequired() {
		return true;
	}

	/**
	 * This class is spawned each time we start monitoring.
	 * It silently monitors an in-memory {@link java.util.concurrent.LinkedBlockingQueue}
	 * for new files and as soon as one is available, delivers it.
	 */
	static class DeliveryRunnable implements Runnable {

		/**
		 *
		 */
		private Logger logger = Logger.getLogger(DeliveryRunnable.class);

		/**
		 * reference to the {@link org.springframework.integration.nativefs.fsmon.AbstractDirectoryMonitor} that can actually deliver newly detected files
		 */
		private AbstractDirectoryMonitor monitor;

		/**
		 * the files detected
		 */
		private LinkedBlockingQueue<File> files;

		/**
		 * the directory under monitor
		 */
		private File directoryUnderMonitor;

		public DeliveryRunnable(AbstractDirectoryMonitor monitor, File dir, LinkedBlockingQueue<File> files) {
			this.monitor = monitor;
			this.files = files;
			this.directoryUnderMonitor = dir;

			Assert.notNull(this.files, "You must pass in a non-empty queue to monitor");
			Assert.notNull(this.monitor, "you must pass in a non-null reference to the parent OsXDirectoryMonitor");
			Assert.notNull(this.directoryUnderMonitor, "You must provide a non-null reference to the directory to monitor");
		}

		/**
		 * this is naturally synchronized in terms of takes because it only runs when there's something in the queue. there is however no guarantee
		 * that iteration won't <em>see</em> a queue containing doubles
		 */
		@Override
		public void run() {
			File f;
			try {
				while ((f = this.files.take()) != null) {

					int countOfFileInQueue = 0;
					for (File fToCount : this.files)
						if (fToCount.equals(f))
							countOfFileInQueue += 1;


					this.monitor.fileReceived(directoryUnderMonitor.getAbsolutePath(), f.getAbsolutePath());
				}

			} catch (InterruptedException e) {
				logger.debug(e);
			}
		}
	}
}

