package com.demo.searchengine.util;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Smart file filter for determining which files should be indexed. Implements configurable file extension filtering
 * and size limits to optimize performance and memory usage.
 */
public class FileFilter {
    private static final Logger logger = Logger.getLogger(FileFilter.class.getName());

    // Default configuration
    private static final long DEFAULT_MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB
    private static final Set<String> DEFAULT_INDEXABLE_EXTENSIONS =
        Set.of(".txt", ".java", ".py", ".js", ".cpp", ".c", ".h", ".hpp", ".xml", ".json", ".yml", ".yaml",
            ".properties", ".md", ".rst", ".sql", ".sh", ".bat", ".ps1", ".gradle", ".mvn", ".pom");

    // Configuration
    private final long maxFileSize;
    private final Set<String> indexableExtensions;
    private final boolean caseSensitive;

    /**
     * Creates a FileFilter with default settings.
     */
    public FileFilter() {
        this(DEFAULT_MAX_FILE_SIZE, DEFAULT_INDEXABLE_EXTENSIONS, false);
    }

    /**
     * Creates a FileFilter with custom settings.
     * @param maxFileSize Maximum file size in bytes
     * @param indexableExtensions Set of file extensions to index
     * @param caseSensitive Whether extension matching is case-sensitive
     */
    public FileFilter(final long maxFileSize, final Set<String> indexableExtensions, final boolean caseSensitive) {
        this.maxFileSize = maxFileSize;
        this.indexableExtensions = indexableExtensions;
        this.caseSensitive = caseSensitive;
    }

    /**
     * Determines if a file should be indexed based on size and extension.
     * @param filePath Path to the file to check
     * @return true if the file should be indexed, false otherwise
     */
    public boolean shouldIndex(final Path filePath) {
        try {
            // Check if the file exists and is a regular file
            if (!Files.exists(filePath) || !Files.isRegularFile(filePath)) {
                return false;
            }

            // Check file size
            final long fileSize = Files.size(filePath);
            if (fileSize > maxFileSize) {
                logger.warning("File ignored (size limit): " + filePath + " (" + formatFileSize(fileSize) + " > "
                        + formatFileSize(maxFileSize) + ")");
                System.out.println("⚠️  File ignored (size limit): " + filePath.getFileName() + " ("
                        + formatFileSize(fileSize) + " > " + formatFileSize(maxFileSize) + ")");
                return false;
            }

            // Check file extension
            final String fileName = filePath.getFileName()
                    .toString();
            final String extension = getFileExtension(fileName);

            if (!hasIndexableExtension(extension)) {
                logger.warning("File ignored (extension): " + filePath + " (extension: " + extension + ")");
                System.out.println("⚠️  File ignored (extension): " + fileName + " (extension: " + extension + ")");
                return false;
            }

            // File passed all checks
            logger.fine("File accepted for indexing: " + filePath + " (" + formatFileSize(fileSize) + ")");
            return true;

        } catch (final Exception e) {
            logger.warning("Error checking file: " + filePath + " - " + e.getMessage());
            return false;
        }
    }

    /**
     * Checks if a file extension is in the indexable set.
     */
    private boolean hasIndexableExtension(final String extension) {
        if (extension == null || extension.isEmpty()) {
            return false;
        }

        if (caseSensitive) {
            return indexableExtensions.contains(extension);
        } else {
            return indexableExtensions.stream()
                    .anyMatch(ext -> ext.equalsIgnoreCase(extension));
        }
    }

    /**
     * Extracts the file extension from a filename.
     */
    private String getFileExtension(final String fileName) {
        final int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex > 0 && lastDotIndex < fileName.length() - 1) {
            return fileName.substring(lastDotIndex);
        }
        return "";
    }

    /**
     * Formats file size in human-readable format.
     */
    private String formatFileSize(final long bytes) {
        if (bytes < 1024)
            return bytes + " B";
        if (bytes < 1024 * 1024)
            return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024)
            return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
        return String.format("%.1f GB", bytes / (1024.0 * 1024.0 * 1024.0));
    }

    /**
     * Gets the current maximum file size limit.
     * @return maximum file size in bytes
     */
    long getMaxFileSize() {
        return maxFileSize;
    }

    /**
     * Gets the current indexable extensions.
     * @return set of indexable extensions
     */
    Set<String> getIndexableExtensions() {
        return Set.copyOf(indexableExtensions);
    }

    /**
     * Gets whether extension matching is case-sensitive.
     * @return true if case-sensitive matching is enabled
     */
    boolean isCaseSensitive() {
        return caseSensitive;
    }

    /**
     * Builder for creating FileFilter instances with custom configuration.
     */
    public static class Builder {
        private long maxFileSize = DEFAULT_MAX_FILE_SIZE;
        private Set<String> indexableExtensions = DEFAULT_INDEXABLE_EXTENSIONS;
        private boolean caseSensitive = false;

        /**
         * Sets maximum file size limit.
         * @param maxFileSize maximum file size in bytes
         * @return this builder
         */
        public Builder maxFileSize(final long maxFileSize) {
            this.maxFileSize = maxFileSize;
            return this;
        }

        /**
         * Sets indexable file extensions.
         * @param indexableExtensions set of extensions to index
         * @return this builder
         */
        public Builder indexableExtensions(final Set<String> indexableExtensions) {
            this.indexableExtensions = Set.copyOf(indexableExtensions);
            return this;
        }

        /**
         * Sets case sensitivity for extension matching.
         * @param caseSensitive true for case-sensitive matching
         * @return this builder
         */
        public Builder caseSensitive(final boolean caseSensitive) {
            this.caseSensitive = caseSensitive;
            return this;
        }

        /**
         * Builds FileFilter instance.
         * @return new FileFilter instance
         */
        public FileFilter build() {
            return new FileFilter(maxFileSize, indexableExtensions, caseSensitive);
        }
    }
}
