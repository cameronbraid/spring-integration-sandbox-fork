package org.springframework.integration.nativefs.fsmon;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

import java.io.File;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;


/**
 * Abstract base class for the other {@link org.springframework.integration.nativefs.fsmon.DirectoryMonitor} implementations.
 *
 * @author Josh Long
 * @since 1.0
 */
public abstract class AbstractDirectoryMonitor implements DirectoryMonitor, DisposableBean, InitializingBean {
	
	protected Logger logger = Logger.getLogger(AbstractDirectoryMonitor.class);

    /**
     * the executor is what's used to manage threading concerns for {@link DirectoryMonitor} implementations. Clients can provide their own, just know that subclasses
     * can "see" this instance and are encouraged to rely on it for their implementation specific use cases.
     */
	protected volatile Executor executor;

    /**
     * registry of {@link File} to {@link FileAddedListener} listeners.
     */
	protected volatile ConcurrentHashMap<File, FileAddedListener> monitors = new ConcurrentHashMap<File, FileAddedListener>();

    /**
     * mapping of directory path's to fully resolved {@link java.io.File} instances. {@link File} references are expensive, so it's valuable to precache them.
     */
	protected Map<String, File> mapOfDirectoriesUnderMonitor = new ConcurrentHashMap<String, File>();

    /**
     * should the object setup the directory on behalf of the client?
     */
	protected boolean autoCreateDirectory = true;

	/**
	 * Make sure there's a directory to startMonitor (one less error condition to have to check in native code)
	 *
	 * @param dir the directory we need to guarantee exists
	 * @return whether or not the directory exists
	 */
	protected boolean ensureExists(File dir) {
		boolean goodDirToMonitor = (dir.isDirectory() && dir.exists());

		Assert.notNull(dir, "the 'dir' parameter must not be null");

		if (!goodDirToMonitor) {
			if (!dir.exists()) {
				if (this.autoCreateDirectory) {
					if (!dir.mkdirs()) {
						logger.debug(String.format(
								"couldn't create directory %s",
								dir.getAbsolutePath()));
					}
				}
			}
		}

		Assert.state(dir.exists(), "the directory " + dir.getAbsolutePath() + " doesn't exist");

		return dir.exists();
	}

	/**
	 * this is the hook we surface for native code to tell the implementation that a file has been observed in one of the watched directories.
	 *
	 * @param dir			the directory under watch
	 * @param fileName the file in the directory under watch
	 */
	public void fileReceived(String dir, String fileName) {
		File dirFile = mapOfDirectoriesUnderMonitor.get(dir);
		this.monitors.get(dirFile).fileAdded(dirFile, fileName);
	}

    /**
     * register a listener for a given directory
     *
     * @param dir the directory for which the {@link FileAddedListener} should be registered / invoked
     * @param fileAddedListener the {@link DirectoryMonitor.FileAddedListener} provides
     */
	@Override
	public void monitor(final File dir, final FileAddedListener fileAddedListener) {
		if (ensureExists(dir)) {
			mapOfDirectoriesUnderMonitor.put(dir.getAbsolutePath(), dir);
			monitors.putIfAbsent(dir, fileAddedListener);
			executor.execute(new Runnable() {
				@Override
				public void run() {
					startMonitor(dir.getAbsolutePath());
				}
			});
		}
	}

    /**
     * tear down the machinery for each monitor
     * @throws Exception
     */
	@Override
	public void destroy() throws Exception {
		for (File file : this.monitors.keySet())
			stopMonitor(file.getAbsolutePath());
	}

	/**
	 * we need an executor to run the polling thread
	 *
	 * @param executor the executor
	 */
	public void setExecutor(Executor executor) {
		this.executor = executor;
	}

    /**
     * use this as a hook to make sure certain things are correctly setup by default (eg: {@link #executor} can't be null)
     * @throws Exception
     */
	@Override
	public void afterPropertiesSet() throws Exception {
		if (this.executor == null) {
			this.executor = Executors.newCachedThreadPool();
		}

		onInit();
	}

	/**
	 * setup machinery for monitoring the directory
	 *
	 * @param path the path to be monitored
	 */
	abstract protected void startMonitor(String path);

	/**
	 * this is the hook that implementations can use to dismantle any native machinery setup for the watch
	 *
	 * @param path the path for whom any watches should de dismantled
	 */
	abstract protected void stopMonitor(String path);

	/**
	 * hook for subclasses to provide initialization logic
	 */
	abstract protected void onInit();
}
