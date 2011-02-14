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
 * Abstract base class for the other {@link DirectoryMonitor} implementations.
 * This class provides hooks for concerns that most implementations will need including event delivery, consumer registration, and
 * a place to stash a  {@link java.util.concurrent.Executor} which implementations may, or may not, take advantage of.
 * <p/>
 * Clients can implement the abstract methods to provide implementation specific logic.
 * <p/>
 * <ol>
 * <li> {@link #startMonitor(String)} will be called when a path is to be monitored. The path is a path as derived from a {@link java.io.File#getAbsolutePath()}.  </li>
 * <li> {@link #stopMonitor(String)}  will be called when a path should no longer be monitored. The path is a path retrieved by {@link java.io.File#getAbsolutePath()}. </li>
 * <li> {@link #onInit()} is a simple callback hook after this class has performed all important setup tasks. </li>
 * </ol>
 * <p/>
 * It's up to each implementation to signal to the implementation that a {@link java.io.File} has been received by calling {@link #fileReceived(String, String)}.
 * <p/>
 * The first parameter is the directory (it should match exactly the value passed to an invocation of {@link #startMonitor(String)} ) and the second
 * should be the name of the file inside that directory (<em>not</em> an absolute path.). This design implies that monitors are only supposed to support
 * detection one level deep. While recursive file detection is a possibility unique to each implementation, it's not required and there's no guarantees made about its consistent support.
 *
 * @author Josh Long
 * @since 1.0
 */
public abstract class AbstractDirectoryMonitor implements DirectoryMonitor, DisposableBean, InitializingBean {

	protected Logger logger = Logger.getLogger(AbstractDirectoryMonitor.class);

	/**
	 * the executor is what's used to manage threading concerns for {@link org.springframework.integration.nativefs.fsmon.DirectoryMonitor} implementations. Clients can provide their own, just know that subclasses
	 * can "see" this instance and are encouraged to rely on it for their implementation specific use cases.
	 */
	protected volatile Executor executor;

	/**
	 * registry of {@link java.io.File} to {@link org.springframework.integration.nativefs.fsmon.DirectoryMonitor.FileAddedListener} listeners.
	 */
	protected volatile ConcurrentHashMap<File, FileAddedListener> monitors = new ConcurrentHashMap<File, FileAddedListener>();

	/**
	 * mapping of directory path's to fully resolved {@link java.io.File} instances. {@link java.io.File} references are expensive, so it's valuable to precache them.
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
	 * @param dir							 the directory for which the {@link org.springframework.integration.nativefs.fsmon.DirectoryMonitor.FileAddedListener} should be registered / invoked
	 * @param fileAddedListener the {@link org.springframework.integration.nativefs.fsmon.DirectoryMonitor.FileAddedListener} provides
	 */
	@Override
	public void monitor(final File dir, final FileAddedListener fileAddedListener) {
		if (ensureExists(dir)) {
			mapOfDirectoriesUnderMonitor.put(dir.getAbsolutePath(), dir);
			monitors.putIfAbsent(dir, fileAddedListener);
			executor.execute(new Runnable() {
				@Override
				public void run() {
					String path = dir.getAbsolutePath();

					startMonitor(path);
				}
			});
		}
	}


	/**
	 * tear down the machinery for each monitor
	 *
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
	 *
	 * @throws Exception
	 */
	@Override
	final public void afterPropertiesSet() throws Exception {
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

	/**
	 * does this dependency require a native library to be loaded via JNI?
	 */
	abstract public boolean isNativeDependencyRequired();
}
