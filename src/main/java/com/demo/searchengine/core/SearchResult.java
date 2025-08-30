package com.demo.searchengine.core;

import java.nio.file.Path;
import java.util.Objects;

/**
 * Represents a search result containing file location and relevance information. This is the primary data structure
 * returned by search operations.
 */
public class SearchResult implements Comparable<SearchResult> {
    private final Path filePath;
    private final String fileName;
    private final int lineNumber;
    private final String matchedText;
    private final long fileSize;

    /**
     * Creates a search result with all information.
     * @param filePath the file path
     * @param lineNumber the line number (must be positive)
     * @param matchedText the matched text
     * @param fileSize the file size in bytes
     * @throws IllegalArgumentException if lineNumber is not positive or fileSize is negative
     */
    public SearchResult(final Path filePath, final int lineNumber, final String matchedText, final long fileSize) {
        this.filePath = Objects.requireNonNull(filePath, "File path cannot be null");
        this.fileName = filePath.getFileName()
                .toString();
        this.lineNumber = lineNumber;
        this.matchedText = Objects.requireNonNull(matchedText, "Matched text cannot be null");
        this.fileSize = fileSize;

        if (lineNumber < 1) {
            throw new IllegalArgumentException("Line number must be positive, got: " + lineNumber);
        }

        if (fileSize < 0) {
            throw new IllegalArgumentException("File size must be non-negative, got: " + fileSize);
        }
    }

    /**
     * Creates a search result with basic information.
     * @param filePath the file path
     * @param lineNumber the line number (must be positive)
     * @param matchedText the matched text
     * @throws IllegalArgumentException if lineNumber is not positive
     */
    public SearchResult(final Path filePath, final int lineNumber, final String matchedText) {
        this(filePath, lineNumber, matchedText, 0);
    }

    /**
     * Returns the file name.
     * @return file name
     */
    public String getFileName() {
        return fileName;
    }

    /**
     * Returns the line number.
     * @return line number
     */
    public int getLineNumber() {
        return lineNumber;
    }

    /**
     * Compare search results by relevance score (higher scores first). If scores are equal, compare by file name and
     * line number.
     */
    @Override
    public int compareTo(final SearchResult other) {
        // Secondary sort: file name
        final int nameComparison = this.fileName.compareTo(other.fileName);
        if (nameComparison != 0) {
            return nameComparison;
        }

        // Tertiary sort: line number
        return Integer.compare(this.lineNumber, other.lineNumber);
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;

        final SearchResult that = (SearchResult) obj;
        return lineNumber == that.lineNumber && fileSize == that.fileSize && Objects.equals(filePath, that.filePath)
                && Objects.equals(matchedText, that.matchedText);
    }

    @Override
    public int hashCode() {
        return Objects.hash(filePath, lineNumber, matchedText, fileSize);
    }

    @Override
    public String toString() {
        return String.format("SearchResult{file=%s, line=%d, text='%s', size=%d}", fileName, lineNumber, matchedText,
            fileSize);
    }
}
