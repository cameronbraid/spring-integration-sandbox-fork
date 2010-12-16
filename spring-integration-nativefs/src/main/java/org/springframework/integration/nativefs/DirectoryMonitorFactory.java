package org.springframework.integration.nativefs;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.integration.nativefs.fsmon.DirectoryMonitor;
import org.springframework.integration.nativefs.fsmon.LinuxInotifyDirectoryMonitor;
import org.springframework.integration.nativefs.fsmon.Nio2WatchServiceDirectoryMonitor;
import org.springframework.integration.nativefs.fsmon.OsXDirectoryMonitor;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

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
public class DirectoryMonitorFactory implements FactoryBean<DirectoryMonitor>, InitializingBean {

    private Logger logger = Logger.getLogger(DirectoryMonitorFactory.class);

    /**
     * not necessarily used by all implementations, but most of them do use it, so we require it.
     */
    private Executor executor;

    /**
     * the executor to be given to all {@link DirectoryMonitor} references
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
     *
     * @return whether or the OS is Linux and whether the kernel supports inotify. basically, is this an appropriate pplace to run the {@link org.springframework.integration.nativefs.fsmon.LinuxInotifyDirectoryMonitor}
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

        if (logger.isInfoEnabled())
            logger.debug("The DirectoryMonitor implementation you are running requires a native dependency on your library path. Typically, this involves" +
                    "selecting the correct dependency (libsifsmon.so on Linux, libsifsmon.dylib on OSX, libsifsmon.dll on Windows) and ensuring it can be" +
                    "found on one of your target system's library paths (well known paths like /user/lib, or a specific path " +
                    "specified by using -Djava.library.path=... on the command line for your JDK invocation).");

    }

    /**
     * detect whether we can use the {@link OsXDirectoryMonitor} implemetnation on this system
     *
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

    @Override
    public DirectoryMonitor getObject() throws Exception {
        DirectoryMonitor dm = null;

        if (this.supportsLinuxInotify()) {
            LinuxInotifyDirectoryMonitor linuxInotifyDirectoryMonitor = new LinuxInotifyDirectoryMonitor();
            linuxInotifyDirectoryMonitor.setExecutor(this.executor);
            linuxInotifyDirectoryMonitor.afterPropertiesSet();
            dm = linuxInotifyDirectoryMonitor;
            notifyNativeDependencyRequired();
        } else if (this.supportsOsXFsEvents()) {
            OsXDirectoryMonitor osxd = new OsXDirectoryMonitor();
            osxd.setExecutor(this.executor);
            osxd.afterPropertiesSet();
            dm = osxd;
            notifyNativeDependencyRequired();

        } else if (this.supportsWindows()) {

            notifyNativeDependencyRequired();

        } else if (this.supportsJdk7WatchService()) {
            /**
             * this code will compile, but not run on operating systems without a JDK7 install. Particularly, we need JDK7's watchservice which,
             * as of this writing, was only available from OpenJDK7 on Linux (presumably for the same reason that our native support was first available
             * on Linux: it's <em>much</em> easier to get that working than the equivalent OSX or Windows code.
             */
            Nio2WatchServiceDirectoryMonitor nio2WatchServiceDirectoryMonitor = new Nio2WatchServiceDirectoryMonitor();
            nio2WatchServiceDirectoryMonitor.setExecutor( this.executor);
            nio2WatchServiceDirectoryMonitor.afterPropertiesSet();
            dm = nio2WatchServiceDirectoryMonitor;
        }

        // z/OS?
        Assert.notNull(dm, "the DirectoryMonitor instance hasn't been intialized. This indicates you " +
                "aren't running on a supported platform (OSX with FsEvents, Linux 2.6 with inotify, " +
                "Windows 2000 or later, or a JDK 7 implementation with java.nio.file.WatchService. " +
                "For operating systems where we have no support for event-based file monitor dispatch (like z/OS), " +
                "please consider a polling solution instead");

        return dm;
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
    }

    public static void main(String[] args) throws Throwable {
        DirectoryMonitorFactory factory = new DirectoryMonitorFactory();
        factory.setExecutor(Executors.newScheduledThreadPool(10));
        factory.afterPropertiesSet();
        DirectoryMonitor dm = factory.getObject();
    }


}

/**
 * utility class to facilitate parsing the system version property, which can be tedious.
 *
 * @author Josh Long
 * @since 2.1
 */
class SystemVersion {

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