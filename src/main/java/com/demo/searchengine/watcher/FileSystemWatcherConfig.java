package com.demo.searchengine.watcher;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Simple configuration for the FileSystemWatcher. Only essential options for now - we'll add more later.
 */
public class FileSystemWatcherConfig {

    private final Set<String> supportedExtensions;
    private final Path watchDirectory;

    private FileSystemWatcherConfig(final Set<String> supportedExtensions, final Path watchDirectory) {
        this.supportedExtensions = new HashSet<>(supportedExtensions);
        this.watchDirectory = watchDirectory;
    }

    /**
     * Returns copy of supported file extensions.
     * @return set of supported extensions
     */
    Set<String> getSupportedExtensions() {
        return new HashSet<>(supportedExtensions);
    }

    /**
     * Check if a file should be watched based on this configuration.
     */
    public boolean shouldWatchFile(final String fileName) {
        if (fileName == null) {
            return false;
        }
        final String lowerFileName = fileName.toLowerCase();
        return supportedExtensions.stream()
                .anyMatch(lowerFileName::endsWith);
    }

    /**
     * Create the default configuration with common text file extensions.
     */
    public static FileSystemWatcherConfig createDefault() {
        final Set<String> extensions =
            new HashSet<>(Arrays.asList(".txt", ".md", ".java", ".py", ".js", ".go", ".sh", ".xml", ".json"));
        return new FileSystemWatcherConfig(extensions, null);
    }

    /**
     * Create minimal configuration with only basic extensions.
     */
    public static FileSystemWatcherConfig createMinimal() {
        final Set<String> extensions = new HashSet<>(Arrays.asList(".txt", ".md", ".java"));
        return new FileSystemWatcherConfig(extensions, null);
    }

    /**
     * Create configuration with custom watch directory.
     */
    public static FileSystemWatcherConfig createWithWatchDirectory(final Path watchDirectory) {
        final Set<String> extensions =
            new HashSet<>(Arrays.asList(".txt", ".md", ".java", ".py", ".js", ".go", ".sh", ".xml", ".json"));
        return new FileSystemWatcherConfig(extensions, watchDirectory);
    }

    @Override
    public String toString() {
        return "FileSystemWatcherConfig{supportedExtensions=" + supportedExtensions + ", watchDirectory="
                + watchDirectory + "}";
    }

    /**
     * Returns thread pool size for file watching.
     * @return thread pool size
     */
    int getThreadPoolSize() {
        return 4;
    }

    /**
     * Returns watch event timeout in milliseconds.
     * @return timeout in milliseconds
     */
    long getWatchEventTimeoutMs() {
        return 500;
    }

    /**
     * Returns the watch directory path.
     * @return watch a directory path
     */
    Path getWatchDirectory() {
        return watchDirectory;
    }
}
