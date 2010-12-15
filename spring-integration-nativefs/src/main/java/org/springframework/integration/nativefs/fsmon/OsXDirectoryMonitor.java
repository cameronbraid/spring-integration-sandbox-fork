package org.springframework.integration.nativefs.fsmon;


import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentSkipListSet;

/**
 * implementation of the {@link org.springframework.integration.nativefs.fsmon.DirectoryMonitor}
 * interface that supports OS X
 *
 * @author Josh Long
 */
public class OsXDirectoryMonitor extends AbstractDirectoryMonitor {

    private static Logger logger = Logger.getLogger(OsXDirectoryMonitor.class);

    static {
        try {
            System.loadLibrary("sifsmon");
        } catch (Throwable t) {
            logger.error("Received exception " + ExceptionUtils.getFullStackTrace(t) + " when trying to load the native library sifsmon");
        }
    }

    @Override
    protected void startMonitor(String path) {
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

    private final Object scanMonitor = new Object();



    /**
     * the mapping of files that we pick up each scan
     */
    private volatile ConcurrentHashMap<String, Collection<File>> statefulMappingOfDirectoryContents = new ConcurrentHashMap<String, Collection<File>>();

    /**
     * scan's the directory and provides a delta for all files that are there now that weren't there in the last scan
     * @param dir
     */
    private void scan(String dir) {

        synchronized (this.scanMonitor) {

            File f = new File(dir);
            if (f.exists() && f.isDirectory()) {
                File[] dirListing = f.listFiles();

                if(dirListing == null) dirListing = new File[0] ;

                this.statefulMappingOfDirectoryContents.putIfAbsent(dir, new ConcurrentSkipListSet<File>());
                Collection<File> files = this.statefulMappingOfDirectoryContents.get(dir);
                Collection<File> deltas = new ArrayList<File>();

                for (File ff : dirListing){
                    if(!files.contains(ff)){
                        deltas.add(ff);
                    }
                }



                // so, for each file in the current listing, see if it exists in the old scan. if not, then add to deltas

               // for(File f : dirListing)


            }
        }
    }

    /**
     * this is the path that has changed. It DOES NOT tell us which files have cahnged, just that there was a change.
     * We need to keep a stateful view of the path and do deltas.
     * This code will be called FROM JNI, so it needs to be very simple.
     *
     * @param path the path that has received the changes
     */
    public synchronized void pathChanged(String path) {
        System.out.println(String.format("the path %s has changed; must rescan!", path));
    }

    @Override
    protected void onInit() {
        this.executor.execute(new Runnable() {

            @Override
            public void run() {
                try {
                    Thread.sleep(1000 * 3);
                    //    System.out.println( "writing a file");
                    File f = new File("/Users/jolong/Desktop/foo");
                    File out = new File(f, "outx.txt");
                    FileOutputStream fout = new FileOutputStream(out);
                    IOUtils.write("Hello, world!", fout);
                    IOUtils.closeQuietly(fout);
                } catch (Throwable th) {
                    System.out.println("Exceptions: " + ExceptionUtils.getFullStackTrace(th));
                }
            }
        });
    }
}
