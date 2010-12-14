package org.springframework.integration.nativefs.fsmon;


import org.apache.commons.lang.SystemUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.log4j.Logger;

import java.io.File;

/***
 *
 * implementation of the {@link org.springframework.integration.nativefs.fsmon.DirectoryMonitor}
 * interface that supports OS X
 *
 * @author Josh Long
 */
public class OsXDirectoryMonitor extends AbstractDirectoryMonitor {

    private static Logger logger = Logger.getLogger(LinuxInotifyDirectoryMonitor.class);

    public static void main(String [] args ) throws Throwable {
        OsXDirectoryMonitor dmOsXDirectoryMonitor = new OsXDirectoryMonitor();
        File desktop = new File(SystemUtils.getUserHome(),"Desktop");
        File foo = new File(desktop, "foo");
        dmOsXDirectoryMonitor.monitor(foo.getAbsolutePath());
    }


	static {
		try {
			System.loadLibrary("sifsmon");
		} catch (Throwable t) {
			logger.error("Received exception " + ExceptionUtils.getFullStackTrace(t) + " when trying to load the native library sifsmon");
		}
	}

    @Override
	protected void startMonitor(String path) {		 
	}

	@Override
	protected void stopMonitor(String path) {
	}


    /**
     * this will delegate to the native .dynlib implementation to register a watch on a directory
     *
     * @param path the path to be monitored
     *
     */
    public native void monitor( String path) ;


    /**
     * this is the path that has changed. It DOES NOT tell us which files have cahnged, just that there was a change.
     * We need to keep a stateful view of the path and do deltas.
     * This code will be called FROM JNI, so it needs to be very simple.
     *
     * @param path the path that's changed
     */
    public void pathChanged(String path){

        System.out.println( String.format( "the path %s has changed; must rescan!" ,path));

    }

	@Override
	protected void onInit() {
	}
}
