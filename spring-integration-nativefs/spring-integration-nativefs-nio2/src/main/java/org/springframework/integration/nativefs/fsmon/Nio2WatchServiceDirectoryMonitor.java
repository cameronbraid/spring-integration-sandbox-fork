package org.springframework.integration.nativefs.fsmon;

import org.apache.log4j.Logger;

import java.io.IOException;
import java.nio.file.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static java.nio.file.StandardWatchEventKind.ENTRY_CREATE;

/**
 * this is an implementation that delegates to a a JDK 7 NIO.2 based WatchService if available.
 * At the time of this writing, the only implementation is the OpenJDK implementation which only works on Linux (AFAIK).
 *
 * @author Josh Long
 * @since 2.1
 */
public class Nio2WatchServiceDirectoryMonitor extends AbstractDirectoryMonitor implements Runnable {

	private Logger logger = Logger.getLogger(Nio2WatchServiceDirectoryMonitor.class);

	public boolean isNativeDependencyRequired() {
		return false;
	}

	/**
	 * JDK NIO2 instance that's used to fire events as file system entries are detected
	 */
	private WatchService watchService;

	/**
	 * if the background loop's been started, then there's no need to start another one. this mutex provides that guarantee.
	 */
	private volatile boolean backgroundLoopStarted;

	/**
	 * we need to make sure that the background processing thread is setup and running only once
	 */
	private final Object backgroundLoopMonitor = new Object();

	/**
	 * mapping between watchkeys (as created when a directory is newly registered with the {@link #watchService}) and {@link Path}
	 */
	private Map<WatchKey, Path> keys = new ConcurrentHashMap<WatchKey, Path>();

	/**
	 * this will be executed inside of a {@link java.lang.Thread} once submitted to a {@link java.util.concurrent.Executor} instance
	 */
	@Override
	public void run() {
		while (true) {

			// wait for key to be signalled
			WatchKey key;
			try {
				key = this.watchService.take();
			} catch (Throwable x) {
				logger.debug("Exception when trying to take key from watchService: ", x);
				return;
			}

			Path dir = keys.get(key);
			if (dir == null) {
				logger.debug("WatchKey not recognized!!");
				continue;
			}

			for (WatchEvent<?> event : key.pollEvents()) {
				WatchEvent.Kind kind = event.kind();
				WatchEvent<Path> ev = cast(event);

				if (kind == ENTRY_CREATE) {

					Path ctx = ev.context();
					String dirStr = dir.toString();
					String fileStr = ctx.getName().toString();

					fileReceived(dirStr, fileStr);

					if (!key.reset())
						keys.remove(key);

				}
			}
		}
	}

	@Override
	protected void onInit() {
		try {
			this.watchService = FileSystems.getDefault().newWatchService();
			synchronized (this.backgroundLoopMonitor) {
				if (!this.backgroundLoopStarted) {
					this.backgroundLoopStarted = true;
					Runnable runnable = this;
					this.executor.execute(runnable);
				}
			}
		} catch (Throwable e) {
			logger.debug("exception when trying to accquire a WatchService", e);
		}
	}

	@Override
	protected void startMonitor(String path) {
		try {
			Path path1 = Paths.get(path);
			WatchKey key = path1.register(this.watchService, ENTRY_CREATE/*, ENTRY_MODIFY*/);
			this.keys.put(key, path1);
		} catch (IOException e) {
			logger.debug("exception when trying to register the path to be watched: " + path);
		}
	}

	@Override
	protected void stopMonitor(String path) {
		/// noop for now
	}

	/**
	 * Picked up this little hack from the JDK 7 samples. EEeWww.
	 *
	 * @param event the event with an untyped generic parameter
	 * @return a type-casted instance of the {@link WatchEvent}
	 */
	@SuppressWarnings("unchecked")
	private <T> WatchEvent<T> cast(WatchEvent<?> event) {
		return (WatchEvent<T>) event;
	}
}
