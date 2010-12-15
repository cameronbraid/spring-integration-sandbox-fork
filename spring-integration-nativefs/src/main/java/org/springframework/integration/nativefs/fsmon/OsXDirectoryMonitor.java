package org.springframework.integration.nativefs.fsmon;


import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.log4j.Logger;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * implementation of the {@link org.springframework.integration.nativefs.fsmon.DirectoryMonitor}
 * interface that supports OS X.
 *
 * This works by registering a monitor using OSX's kernel facilities to receive callbacks of file system events.
 *
 * As OSX tells this class that <em>something</em> has changed in the underlying file system, we must manually
 * deliver deltas by scanning and calculating what's new.
 *
 * Meanwhile, in delivery threads (one for each directory monitored by this class) we deliver these new files and trigger listeners.
 *
 * @author Josh Long
 */
public class OsXDirectoryMonitor extends AbstractDirectoryMonitor {

    private static Logger logger = Logger.getLogger(OsXDirectoryMonitor.class);

    /**
     * the mapping of files that we pick up each scanForNewFiles - each file maintains a queue of files to be delivered
     */
    private volatile ConcurrentHashMap<File, LinkedBlockingQueue<File>> statefulMappingOfDirectoryContents =
            new ConcurrentHashMap<File, LinkedBlockingQueue<File>>();

    static {
        try {
            System.loadLibrary("sifsmon");
        } catch (Throwable t) {
            logger.error("Received exception " + ExceptionUtils.getFullStackTrace(t) + " when trying to load the native library sifsmon");
        }
    }


    @Override
    protected void startMonitor(String path) {

        // OSX specific adaptation because the kernel delivers events with file system paths that end with '/' so we need to be able to match that
        if (path != null && !path.endsWith("/"))
            path = path + "/";

        File f = new File(path);

        /**
         * do one quick scan and get everything preloaded
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
     */
    private Set<File> scanForNewFiles(File theDirectoryToScan) {

        Collection<File> deltas = new ArrayList<File>();

        if (theDirectoryToScan.exists() && theDirectoryToScan.isDirectory()) {

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
        }


        return new HashSet<File>(deltas);
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

        File key = new File(path);
        Set<File> newFiles = this.scanForNewFiles( key);

        Queue<File> queueOfFiles = this.statefulMappingOfDirectoryContents.get(key);

        // enqueue the new files
        for(File f : newFiles)
            queueOfFiles.add( f);


    }


    @Override
    protected void onInit() {
    }
}


class DeliveryRunnable implements Runnable {

    /**
     *
     */
    private Logger logger = Logger.getLogger(DeliveryRunnable.class);

    /**
     * reference to the {@link AbstractDirectoryMonitor} that can actually deliver newly detected files
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
    }

    @Override
    public void run() {
        File f;
        try {
            while ((f = this.files.take()) != null)
                this.monitor.fileReceived(directoryUnderMonitor.getAbsolutePath(), f.getAbsolutePath());

        } catch (InterruptedException e) {
            logger.debug(e);
        }
    }
}