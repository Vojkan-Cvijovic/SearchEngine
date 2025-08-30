package com.demo.searchengine.core;

import java.nio.file.Path;
import java.time.Instant;
import java.util.Objects;

/**
 * Metadata about an indexed file, including size, modification time, and indexing statistics.
 */
public class FileMetadata {
    private final Path filePath;
    private final String fileName;
    private final long fileSize;
    private final Instant lastModified;
    private final Instant indexedAt;
    private final int totalTerms;
    private final int uniqueTerms;

    /**
     * Creates file metadata with all information.
     * @param filePath the file path
     * @param fileSize the file size in bytes
     * @param lastModified last modification time
     * @param totalTerms total number of terms
     * @param uniqueTerms number of unique terms
     * @throws IllegalArgumentException if any values are invalid
     */
    public FileMetadata(final Path filePath, final long fileSize, final Instant lastModified, final int totalTerms,
        final int uniqueTerms) {
        this.filePath = Objects.requireNonNull(filePath, "File path cannot be null");
        this.fileName = filePath.getFileName()
                .toString();
        this.fileSize = fileSize;
        this.lastModified = Objects.requireNonNull(lastModified, "Last modified time cannot be null");
        this.indexedAt = Instant.now();
        this.totalTerms = totalTerms;
        this.uniqueTerms = uniqueTerms;

        if (fileSize < 0) {
            throw new IllegalArgumentException("File size must be non-negative, got: " + fileSize);
        }
        if (totalTerms < 0) {
            throw new IllegalArgumentException("Total terms must be non-negative, got: " + totalTerms);
        }
        if (uniqueTerms < 0) {
            throw new IllegalArgumentException("Unique terms must be non-negative, got: " + uniqueTerms);
        }
        if (uniqueTerms > totalTerms) {
            throw new IllegalArgumentException("Unique terms cannot exceed total terms");
        }
    }

    /**
     * Creates file metadata with basic information.
     * @param filePath the file path
     * @param fileSize the file size in bytes
     * @param lastModified last modification time
     */
    public FileMetadata(final Path filePath, final long fileSize, final Instant lastModified) {
        this(filePath, fileSize, lastModified, 0, 0);
    }

    /**
     * Returns the file path.
     * @return file a path
     */
    public Path getFilePath() {
        return filePath;
    }

    /**
     * Returns the file name.
     * @return file name
     */
    public String getFileName() {
        return fileName;
    }

    /**
     * Returns the file size in bytes.
     * @return file size
     */
    public long getFileSize() {
        return fileSize;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;

        final FileMetadata that = (FileMetadata) obj;
        return fileSize == that.fileSize && totalTerms == that.totalTerms && uniqueTerms == that.uniqueTerms
                && Objects.equals(filePath, that.filePath) && Objects.equals(lastModified, that.lastModified)
                && Objects.equals(indexedAt, that.indexedAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(filePath, fileSize, lastModified, indexedAt, totalTerms, uniqueTerms);
    }

    @Override
    public String toString() {
        return String.format("FileMetadata{file=%s, size=%d, terms=%d/%d, indexed=%s}", fileName, fileSize,
            uniqueTerms, totalTerms, indexedAt);
    }
}
