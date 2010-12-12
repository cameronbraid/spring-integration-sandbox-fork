package org.springframework.integration.nativefs.fsmon;

import org.apache.log4j.Logger;

import java.io.IOException;
import java.nio.file.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static java.nio.file.StandardWatchEventKind.*;

/**
 * this will be an implementation that delegates to a a JDK 7 NIO.2 based WatchService if available
 *
 * @author Josh Long
 * @since 2.1
 */
public class Nio2WatchServiceDirectoryMonitor extends AbstractDirectoryMonitor {

    private Logger logger = Logger.getLogger(Nio2WatchServiceDirectoryMonitor.class);

    private WatchService watchService;

    private Map<WatchKey, Path> keys = new ConcurrentHashMap<WatchKey, Path>();

    @Override
    protected void onInit() {
        try {
            this.watchService = FileSystems.getDefault().newWatchService();
        } catch (IOException e) {
            logger.debug("exception when trying to accquire a WatchService", e);
        }
    }


    @Override
    protected void startMonitor(String path) {
        try {
            this.register(Paths.get(path));
        } catch (IOException e) {
            logger.debug("exception when trying to register the path to be watched: " + path);
        }
    }

    @Override
    protected void stopMonitor(String path) {
        //  noop
    }

    protected void register(Path path) throws IOException {
        WatchKey key = path.register(this.watchService, ENTRY_CREATE, ENTRY_MODIFY);
        this.keys.put(key, path);
    }

    /**
     * Picked up this little hack from the JDK 7 samples. EEeWww.
     */
    @SuppressWarnings("unchecked")
    private <T> WatchEvent<T> cast(WatchEvent<?> event) {
        return (WatchEvent<T>) event;
    }


    /**
     * this will be run inside a thread
     */
    protected void processEvents() {
        while (true) {

            // wait for key to be signalled
            WatchKey key;
            try {
                key = this.watchService.take();
            } catch (InterruptedException x) {
                return;
            }

            Path dir = keys.get(key);
            if (dir == null) {
                logger.debug("WatchKey not recognized!!");
                continue;
            }

            for (WatchEvent<?> event : key.pollEvents()) {
                WatchEvent.Kind kind = event.kind();

                // TBD - provide example of how OVERFLOW event is handled
                if (kind == OVERFLOW) {
                    continue;
                }

                // Context for directory entry event is the file name of entry
                WatchEvent<Path> ev = cast(event);
                Path name = ev.context();
                Path child = dir.resolve(name);

                // print out event
                logger.debug(String.format("%s: %s\n", event.kind().name(), child));


            }

            // reset key and remove from set if directory no longer accessible
            boolean valid = key.reset();
            if (!valid) {
                keys.remove(key);

                // all directories are inaccessible
                if (keys.isEmpty()) {
                    break;
                }
            }
        }
    }


    /*    @SuppressWarnings("unchecked")
             static <T> WatchEvent<T> cast(WatchEvent<?> event) {
                     return (WatchEvent<T>)event;
             }

             private void register(Path dir) throws IOException {
                     WatchKey key = dir.register(watcher, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);
                     if (trace) {
                             FileRef prev = keys.get(key);
                             if (prev == null) {
                                     System.out.format("register: %s\n", dir);
                             } else {
                                     if (!dir.equals(prev)) {
                                             System.out.format("update: %s -> %s\n", prev, dir);
                                     }
                             }
                     }
                     keys.put(key, dir);
             }

             private void registerAll(final Path start) throws IOException {
                     // register directory and sub-directories
                     Files.walkFileTree(start, new SimpleFileVisitor<Path>() {
                             @Override
                             public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)
                                     throws IOException
                             {
                                     register(dir);
                                     return FileVisitResult.CONTINUE;
                             }
                     });
             }


             WatchDir(Path dir, boolean recursive) throws IOException {
                     this.watcher = FileSystems.getDefault().newWatchService();
                     this.keys = new HashMap<WatchKey,Path>();
                     this.recursive = recursive;

                     if (recursive) {
                             System.out.format("Scanning %s ...\n", dir);
                             registerAll(dir);
                             System.out.println("Done.");
                     } else {
                             register(dir);
                     }

                     // enable trace after initial registration
                     this.trace = true;
             }

             void processEvents() {
                     for (;;) {

                             // wait for key to be signalled
                             WatchKey key;
                             try {
                                     key = watcher.take();
                             } catch (InterruptedException x) {
                                     return;
                             }

                             Path dir = keys.get(key);
                             if (dir == null) {
                                     System.err.println("WatchKey not recognized!!");
                                     continue;
                             }

                             for (WatchEvent<?> event: key.pollEvents()) {
                                     WatchEvent.Kind kind = event.kind();

                                     // TBD - provide example of how OVERFLOW event is handled
                                     if (kind == OVERFLOW) {
                                             continue;
                                     }

                                     // Context for directory entry event is the file name of entry
                                     WatchEvent<Path> ev = cast(event);
                                     Path name = ev.context();
                                     Path child = dir.resolve(name);

                                     // print out event
                                     System.out.format("%s: %s\n", event.kind().name(), child);

                                     // if directory is created, and watching recursively, then
                                     // register it and its sub-directories
                                     if (recursive && (kind == ENTRY_CREATE)) {
                                             try {
                                                     if (Attributes.readBasicFileAttributes(child, NOFOLLOW_LINKS).isDirectory()) {
                                                             registerAll(child);
                                                     }
                                             } catch (IOException x) {
                                                     // ignore to keep sample readbale
                                             }
                                     }
                             }

                             // reset key and remove from set if directory no longer accessible
                             boolean valid = key.reset();
                             if (!valid) {
                                     keys.remove(key);

                                     // all directories are inaccessible
                                     if (keys.isEmpty()) {
                                             break;
                                     }
                             }
                     }
             }

             static void usage() {
                     System.err.println("usage: java WatchDir [-r] dir");
                     System.exit(-1);
             }

             public static void main(String[] args) throws IOException {
                     // parse arguments
                     if (args.length == 0 || args.length > 2)
                             usage();
                     boolean recursive = false;
                     int dirArg = 0;
                     if (args[0].equals("-r")) {
                             if (args.length < 2)
                                     usage();
                             recursive = true;
                             dirArg++;
                     }

                     // register directory and process its events
                     Path dir = Paths.get(args[dirArg]);
                     new WatchDir(dir, recursive).processEvents();
             }*/

}
