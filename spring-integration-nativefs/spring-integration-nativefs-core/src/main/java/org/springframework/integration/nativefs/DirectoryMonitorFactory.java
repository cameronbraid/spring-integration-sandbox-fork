package org.springframework.integration.nativefs;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.*;
import org.springframework.integration.nativefs.fsmon.AbstractDirectoryMonitor;
import org.springframework.integration.nativefs.fsmon.DirectoryMonitor;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;


/**
 * We can spare the user pain by dynamically loading the smartest implementation available for their host operating system and JDK.
 * <p/>
 * This will correctly detect support for JDK 7 WatchService implementations, OSX FSEvents, Linux Inotify and Windows.
 * <p/>
 * Ideally, the JDK 7 implementation will be picked up if available (as of late 2010, it's only available in Linux OpenJDK builds). Failing that, however, the native operating system based implementations will enserviced.
 * <p/>
 * Users on operating systems that support neither native event dispatch nor JDK 7 (like z/OS) should consider a polling based alternative.
 * <p/>
 * todo: we should investigate implementing this on BSD. BSD *does* support native event based dispatch, but at the moment we have no support for it.
 * todo: investigate Solaris support (I'm unsure as to whether there exists a working native event-based dispatcher mechanism)
 *
 * @author Josh Long
 * @since 2.1
 */
public class DirectoryMonitorFactory implements FactoryBean<DirectoryMonitor>, InitializingBean, BeanFactoryAware {

	private DirectoryMonitor directoryMonitor;

	private BeanFactory beanFactory;

	private Log log = LogFactory.getLog(getClass());

	/**
	 * not necessarily used by all implementations, but most of them do use it, so we require it.
	 */
	private Executor executor;

	/**
	 * the executor to be given to all {@link org.springframework.integration.nativefs.fsmon.DirectoryMonitor} references
	 *
	 * @param ex the executor
	 */
	public void setExecutor(Executor ex) {
		this.executor = ex;
	}

	/**
	 * is JDK 7 NIO.2 support for WatchServices available?
	 *
	 * @return whether or not the JDK java.nio.file.WatchService hierarchy is available.
	 */
	protected boolean supportsJdk7WatchService() {
		String watchServiceAvailable = "java.nio.file.WatchService";

		boolean hasWatchServiceClazz = true;

		try {
			ClassUtils.forName(watchServiceAvailable, ClassUtils.getDefaultClassLoader());
		} catch (Exception e) {
			hasWatchServiceClazz = false;
		}

		return hasWatchServiceClazz;
	}

	/**
	 * tells whether or not the host operating system is Linux and supports dispatching using the inotify
	 * kernel facility. inotify has been available in Linux kernel since 2.6.13
	 */
	protected boolean supportsLinuxInotify() {

		boolean inotifySupported = false;
		String os = System.getProperty("os.name");
		SystemVersion v = SystemVersion.getSystemVersion();

		if (os.toLowerCase().indexOf("linux") != -1) {
			if (v.getMicroVersion() != 0) {
				int majorVersion = v.getMajorVersion();
				int minorVersion = v.getMinorVersion();
				int microVersion = v.hasMicroVersion() ? v.getMicroVersion() : -1;


				inotifySupported = ((majorVersion > 2) ||
						((majorVersion == 2) && (minorVersion > 6)) ||
						((majorVersion == 2) && (minorVersion == 6) &&
								(microVersion >= 13)));
			}
		}

		return inotifySupported;
	}

	/**
	 * most of these implementations require a native dependency. this message should remind people of the correct use for these native dependencies.
	 */
	protected void notifyNativeDependencyRequired() {

		if (log.isInfoEnabled())
			log.debug("The DirectoryMonitor implementation you are running requires a native dependency on your library path. Typically, this involves" +
					"selecting the correct dependency (libsifsmon.so on Linux, libsifsmon.dylib on OSX, libsifsmon.dll on Windows) and ensuring it can be" +
					"found on one of your target system's library paths (well known paths like /user/lib, or a specific path " +
					"specified by using -Djava.library.path=... on the command line for your JDK invocation).");

	}


	/**
	 * @return whether or not the host operating system (OS X) supports FSevents (10.5 or better will)
	 */
	protected boolean supportsOsXFsEvents() {
		String os = System.getProperty("os.name");
		//Mac OS X and 10.6.5

		if (os.toLowerCase().indexOf("os x") != -1) {
			SystemVersion sv = SystemVersion.getSystemVersion();
			if (sv.getMajorVersion() >= 10 && sv.getMinorVersion() >= 5)
				return true;
		}

		return false;
	}

	/**
	 * todo add support for Windows
	 *
	 * @return whether or not the target OS is windows and supports the correct APIs to make this work.
	 */

	protected boolean supportsWindows() {
		return false;
	}

	/**
	 * builds a map of well-known {@link DirectoryMonitor} implementations and whether or not that implementation, if it was available, could work on this system..
	 * <p/>
	 * This method does not test whether or not that implementation actually exists, though.
	 *
	 * @return map of known implementations => whether or not that implementation could work on ths system
	 */
	private Map<String, Boolean> supportedInstances() {
		Map<String, Boolean> mapOfSupportedMonitors = new HashMap<String, Boolean>();
		String pkg = AbstractDirectoryMonitor.class.getName();
		pkg = pkg.substring(0, pkg.lastIndexOf(".")) + ".";
		mapOfSupportedMonitors.put(pkg + "LinuxInotifyDirectoryMonitor", supportsLinuxInotify());
		mapOfSupportedMonitors.put(pkg + "OsXDirectoryMonitor", supportsOsXFsEvents());
		mapOfSupportedMonitors.put(pkg + "Nio2WatchServiceDirectoryMonitor", supportsJdk7WatchService());
		mapOfSupportedMonitors.put(pkg + "WindowsDirectoryMonitor", supportsWindows());
		return mapOfSupportedMonitors;
	}

	private Set<DirectoryMonitor> resolveSupportedDirectoryMonitorImplementation() {

		Set<DirectoryMonitor> monitors = new HashSet<DirectoryMonitor>();
		Map<String, Boolean> supportedMonitors = this.supportedInstances();
		for (String clazzName : supportedMonitors.keySet()) {
			Object o;
			boolean supported = supportedMonitors.get(clazzName);
			if (supported) {
				try {
					o = Class.forName(clazzName);
					if (o instanceof DirectoryMonitor) {
						DirectoryMonitor directoryMonitor = (DirectoryMonitor) o;
						monitors.add(directoryMonitor);
						if (log.isDebugEnabled())
							log.debug(clazzName + " is supported, and exists on the classpath");
					}
				} catch (Throwable t) {
					log.warn("could not load " + clazzName +
							", and encountered an exception when " +
							"trying to create an instance of it. " +
							"Please ensure that " + clazzName +
							" is on your CLASSPATH.", t);

				}
			}

		}
		return monitors;

	}

	@Override
	public DirectoryMonitor getObject() throws Exception {
		return directoryMonitor;
	}

	@Override
	public Class<?> getObjectType() {
		return DirectoryMonitor.class;
	}

	@Override
	public boolean isSingleton() {
		return true;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		Assert.notNull(this.executor, "the executor can't be null");

		Set<DirectoryMonitor> directoryMonitors = this.resolveSupportedDirectoryMonitorImplementation(); // defaults

		// any configured types
		if(this.beanFactory instanceof ListableBeanFactory){
			ListableBeanFactory listableBeanFactory  = (ListableBeanFactory) this.beanFactory ;
			Map<String,DirectoryMonitor> monitorsInContext = listableBeanFactory.getBeansOfType(DirectoryMonitor.class);
			directoryMonitors.addAll( monitorsInContext.values());
		}
		// make sure we dont accidentally get two instances of the same class
		Map<Class,DirectoryMonitor> dedupedInstances = new HashMap <Class,DirectoryMonitor>() ;
		for(DirectoryMonitor directoryMonitor  : directoryMonitors){
			dedupedInstances.put( directoryMonitor.getClass(),directoryMonitor);
		}

		// at this point its possible we have several, and indeed several of the same type with differing configuration
		directoryMonitors = new HashSet<DirectoryMonitor>(dedupedInstances.values());

		Assert.isTrue(directoryMonitors.size() == 1, "you must have " +
				"exactly one compatible " + DirectoryMonitor.class.getName() +
				"implementation should be on the classpath, but instead there are " + directoryMonitors.size() +
				" implementations. A compatible implementation is " +
				"one that works on both this platform and JDK revision. " +
				"If, for example, you are running on both JDK 7 and " +
				"Linux, you might have 2 implementations available, so exclude one. " +
				((directoryMonitors.size() == 0) ?
						"Since it appears you have no compatible implementations, check that you " +
								"are running on a supported platform (OSX with FsEvents, Linux 2.6 with inotify, " +
								"Windows 2000 or later, or a JDK 7 implementation with java.nio.file.WatchService. " +
								"For operating systems where we have no support for event-based file monitor dispatch (like z/OS), " +
								"please consider a polling solution instead" : ""));

		DirectoryMonitor monitor = directoryMonitors.iterator().next();

		if (monitor instanceof AbstractDirectoryMonitor) {
			AbstractDirectoryMonitor abstractDirectoryMonitor = ((AbstractDirectoryMonitor) monitor);
			abstractDirectoryMonitor.setExecutor(this.executor);
			if (abstractDirectoryMonitor.isNativeDependencyRequired())
				notifyNativeDependencyRequired();
		}

		if (monitor instanceof InitializingBean) {
			((InitializingBean) monitor).afterPropertiesSet();
		}

		this.directoryMonitor = monitor;

	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		this.beanFactory = beanFactory;
	}


	/**
	 * utility class to facilitate parsing the system version property, which can be tedious.
	 *
	 * @author Josh Long
	 * @since 2.1
	 */
	private static class SystemVersion {

		private boolean hasMicro;
		private int major, minor, micro;

		public static SystemVersion getSystemVersion() {
			return new SystemVersion(System.getProperty("os.version"));
		}

		public boolean hasMicroVersion() {
			return this.hasMicro;
		}

		public int getMajorVersion() {
			return this.major;
		}

		public int getMinorVersion() {
			return this.minor;
		}

		public int getMicroVersion() {
			return this.micro;
		}

		public SystemVersion(String version) {
			String[] versionPieces = version.split("\\.");
			if (versionPieces.length >= 2) {
				int majorVersion = Integer.parseInt(versionPieces[0]);
				int minorVersion = Integer.parseInt(versionPieces[1]);
				int microVersion = 0;

				if (versionPieces.length > 2) {
					String[] microVersionPieces = versionPieces[2].split("-");

					if (microVersionPieces.length > 0) {
						microVersion = Integer.parseInt(microVersionPieces[0]);
						hasMicro = true;
					}
				}

				this.major = majorVersion;
				this.micro = microVersion;
				this.minor = minorVersion;

			}
		}
	}
}