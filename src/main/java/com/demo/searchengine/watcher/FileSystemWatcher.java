package com.demo.searchengine.watcher;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.demo.searchengine.service.TextIndexingService;
import com.demo.searchengine.util.RetryUtils;

/**
 * Monitors filesystem changes and automatically updates the search index. Supports recursive directory watching and
 * handles file creation, modification, and deletion.
 */
public class FileSystemWatcher implements AutoCloseable {

    private static final Logger logger = LogManager.getLogger(FileSystemWatcher.class);

    /**
     * Sets the indexing service for this watcher.
     * @param indexingService the indexing service to use
     */
    void setIndexingService(final TextIndexingService indexingService) {
        this.indexingService = indexingService;
    }

    /**
     * Sets the configuration for this watcher.
     * @param config the configuration to use
     */
    void setConfig(final FileSystemWatcherConfig config) {
        this.config = config;
    }

    private TextIndexingService indexingService;
    private FileSystemWatcherConfig config;
    private WatchService watchService;
    private ExecutorService executorService;
    private final AtomicBoolean isRunning;
    private final AtomicInteger watchedDirectories;

    /**
     * Creates new watcher without indexing service.
     */
    public FileSystemWatcher() {
        isRunning = new AtomicBoolean(false);
        watchedDirectories = new AtomicInteger(0);
    }

    /**
     * Creates watcher with indexing service and default config.
     * @param indexingService the indexing service to use
     * @throws IOException if watch service cannot be created
     */
    public FileSystemWatcher(final TextIndexingService indexingService) throws IOException {
        this(indexingService, FileSystemWatcherConfig.createDefault());
    }

    /**
     * Creates watcher with indexing service and custom config.
     * @param indexingService the indexing service to use
     * @param config the configuration to use
     * @throws IOException if watch service cannot be created
     */
    public FileSystemWatcher(final TextIndexingService indexingService, final FileSystemWatcherConfig config)
        throws IOException {
        this.indexingService = indexingService;
        this.config = config;
        this.watchService = FileSystems.getDefault()
                .newWatchService();
        this.executorService = Executors.newFixedThreadPool(config.getThreadPoolSize());
        this.isRunning = new AtomicBoolean(false);
        this.watchedDirectories = new AtomicInteger(0);

        logger.info("FileSystemWatcher initialized with config: {}", config);
    }

    /**
     * Setter for WatchService - used for testing
     */
    void setWatchService(final WatchService watchService) {
        this.watchService = watchService;
    }

    /**
     * Setter for ExecutorService - used for testing
     */
    void setExecutorService(final ExecutorService executorService) {
        this.executorService = executorService;
    }

    /**
     * Start watching a directory recursively for filesystem changes.
     * @param rootDirectory The root directory to watch
     * @param indexExistingFiles Whether to index existing files before starting to watch
     * @throws IOException If watching cannot be established
     */
    public void startWatching(final Path rootDirectory, final boolean indexExistingFiles) throws IOException {
        if (!exists(rootDirectory)) {
            throw new IllegalArgumentException("Directory does not exist: " + rootDirectory);
        }

        if (!isDirectory(rootDirectory)) {
            throw new IllegalArgumentException("Path is not a directory: " + rootDirectory);
        }

        logger.info("Starting filesystem watching for: {}", rootDirectory);

        // Optional index existing files first
        if (indexExistingFiles) {
            logger.info("Indexing existing files before starting to watch...");

            final Integer indexedCount =
                RetryUtils.retryWithExponentialBackoff(() -> indexingService.indexAllFilesInDirectory(rootDirectory),
                    "index existing files", rootDirectory.toString(), false);
            if (indexedCount != null) {
                logger.info("Indexed {} existing files", indexedCount);
            } else {
                logger.error("Failed to index existing files after all retries, continuing with watching");
            }
        }

        // Register the root directory and all subdirectories
        registerDirectoryRecursively(rootDirectory);

        // Start the watching thread
        startWatchingThread();

        logger.info("Filesystem watching started successfully");
    }

    boolean isDirectory(final Path rootDirectory) {
        return Files.isDirectory(rootDirectory);
    }

    boolean exists(final Path rootDirectory) {
        return Files.exists(rootDirectory);
    }

    /**
     * Register a directory and all its subdirectories for watching.
     */
    void registerDirectoryRecursively(final Path directory) throws IOException {
        // Register current directory
        registerDirectory(directory);

        // Walk through subdirectories and register them if configured
        Files.walkFileTree(directory, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(final Path dir, final BasicFileAttributes attrs) {
                if (!dir.equals(directory)) { // Skip root directory (already registered)
                    registerDirectory(dir);
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(final Path file, final IOException exc) {
                logger.warn("Failed to visit file: {} - {}", file, exc.getMessage());
                return FileVisitResult.CONTINUE;
            }
        });

    }

    /**
     * Register a single directory for watching.
     */
    void registerDirectory(final Path directory) {
        RetryUtils.retryWithExponentialBackoff(() -> {
            directory.register(watchService, StandardWatchEventKinds.ENTRY_CREATE,
                StandardWatchEventKinds.ENTRY_DELETE, StandardWatchEventKinds.ENTRY_MODIFY);
            return null;
        }, "register directory for watching", directory.toString(), true);

        watchedDirectories.incrementAndGet();
        logger.info("Registered directory for watching: {}", directory);
    }

    /**
     * Start the main watching thread.
     */
    void startWatchingThread() {
        if (isRunning.compareAndSet(false, true)) {
            executorService.submit(this::watchLoop);
            logger.info("Watching thread started");
        }
    }

    /**
     * Main watching loop that processes filesystem events.
     */
    void watchLoop() {
        logger.info("FileSystemWatcher watching loop started");

        try {
            while (isWatcherRunning()) {
                try {
                    final WatchKey key = watchService.poll(config.getWatchEventTimeoutMs(),
                        java.util.concurrent.TimeUnit.MILLISECONDS);

                    if (key == null) {
                        continue; // Timeout, check if we should continue
                    }

                    final Path watchedDirectory = (Path) key.watchable();
                    processWatchEvents(key, watchedDirectory);

                    if (!key.reset()) {
                        logger.warn("Watch key could not be reset for: {}", watchedDirectory);
                        break;
                    }
                } catch (final java.nio.file.ClosedWatchServiceException e) {
                    // WatchService was closed, exit the loop gracefully
                    logger.info("WatchService closed, stopping watch loop");
                    break;
                } catch (final InterruptedException e) {
                    logger.info("Watching thread interrupted");
                    Thread.currentThread()
                            .interrupt();
                    break;
                }
            }
        } catch (final Exception e) {
            logger.error("Error in watching loop", e);
        } finally {
            logger.info("Watching loop ended");
        }
    }

    boolean isWatcherRunning() {
        return isRunning.get();
    }

    /**
     * Process watch events for a specific directory.
     */
    void processWatchEvents(final WatchKey key, final Path watchedDirectory) {
        for (final WatchEvent<?> event : key.pollEvents()) {
            final WatchEvent.Kind<?> kind = event.kind();

            if (kind == StandardWatchEventKinds.OVERFLOW) {
                continue; // Ignore overflow events
            }

            @SuppressWarnings("unchecked")
            final WatchEvent<Path> ev = (WatchEvent<Path>) event;
            final Path fileName = ev.context();
            final Path fullPath = watchedDirectory.resolve(fileName);

            try {
                processFileEvent(kind, fullPath);
            } catch (final Exception e) {
                logger.error("Error processing file event for: {}", fullPath, e);
            }
        }
    }

    /**
     * Process a single file event.
     */
    void processFileEvent(final WatchEvent.Kind<?> kind, final Path fullPath) {
        if (kind == StandardWatchEventKinds.ENTRY_CREATE) {
            handleFileCreated(fullPath);
        } else if (kind == StandardWatchEventKinds.ENTRY_DELETE) {
            handleFileDeleted(fullPath);
        } else if (kind == StandardWatchEventKinds.ENTRY_MODIFY) {
            handleFileModified(fullPath);
        }
    }

    /**
     * Handle file creation event.
     */
    void handleFileCreated(final Path filePath) {
        if (isDirectory(filePath)) {
            // New directory created - register it for watching
            if (filePath.startsWith(getWatchedDirectory())) {
                try {
                    registerDirectoryRecursively(filePath);
                    logger.info("New directory detected and registered for watching: {}", filePath);
                } catch (final IOException e) {
                    logger.error("Failed to register new directory for watching: {}", filePath, e);
                }
            }
        } else if (isIndexableFile(filePath)) {
            // New file created - index it
            try {
                final boolean success = indexingService.indexFile(filePath);
                if (success) {
                    logger.info("New file indexed: {}", filePath);
                } else {
                    logger.warn("Failed to index new file: {}", filePath);
                }
            } catch (final Exception e) {
                logger.error("Error indexing new file: {}", filePath, e);
            }
        } else {
            logger.info("Modified file is not indexable, skipping: {}", filePath);
        }
    }

    Path getWatchedDirectory() {
        return config.getWatchDirectory();
    }

    /**
     * Handle file deletion event.
     */
    void handleFileDeleted(final Path filePath) {
        try {
            final boolean success = indexingService.removeFile(filePath);
            if (success) {
                logger.info("File removed from index: {}", filePath);
            } else {
                logger.info("File not found in index (already removed): {}", filePath);
            }
        } catch (final Exception e) {
            logger.error("Error removing file from index: {}", filePath, e);
        }
    }

    /**
     * Handle file modification event.
     */
    void handleFileModified(final Path filePath) {
        if (isIndexableFile(filePath)) {
            try {
                // Re-index the modified file
                final boolean success = indexingService.indexFile(filePath);
                if (success) {
                    logger.info("Modified file re-indexed: {}", filePath);
                } else {
                    logger.warn("Failed to re-index modified file: {}", filePath);
                }
            } catch (final Exception e) {
                logger.error("Error re-indexing modified file: {}", filePath, e);
            }
        } else {
            logger.info("Modified file is not indexable, skipping reindexing: {}", filePath);
        }
    }

    /**
     * Check if a file should be indexed.
     */
    boolean isIndexableFile(final Path filePath) {
        if (Files.isDirectory(filePath)) {
            return false;
        }

        return config.shouldWatchFile(filePath.getFileName()
                .toString());
    }

    /**
     * Stop watching and clean up resources.
     */
    public void stop() {
        logger.info("Stopping FileSystemWatcher...");
        isRunning.set(false);

        // Shutdown executor service first to stop the watching thread
        if (executorService != null) {
            executorService.shutdown();
            try {
                // Wait for threads to finish, but don't wait forever
                if (!executorService.awaitTermination(2, java.util.concurrent.TimeUnit.SECONDS)) {
                    executorService.shutdownNow();
                    if (!executorService.awaitTermination(1, java.util.concurrent.TimeUnit.SECONDS)) {
                        logger.warn("Executor service did not terminate");
                    }
                }
            } catch (final InterruptedException e) {
                executorService.shutdownNow();
                Thread.currentThread()
                        .interrupt();
            }
        }

        // Close watch service after threads are stopped
        try {
            if (watchService != null) {
                watchService.close();
            }
        } catch (final IOException e) {
            logger.error("Error closing watch service", e);
        }

        logger.info("FileSystemWatcher stopped");
    }

    /**
     * Get the number of directories currently being watched.
     */
    public int getWatchedDirectoriesCount() {
        return watchedDirectories.get();
    }

    /**
     * Close the watcher and release all resources.
     */
    @Override
    public void close() {
        stop();
    }

}
