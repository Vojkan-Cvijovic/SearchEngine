package com.demo.searchengine.core;

import java.nio.file.Path;
import java.util.Objects;

/**
 * Represents the location of a term within a file. This includes the file path, line number, and position within the
 * line.
 */
public record FileLocation(Path filePath, int lineNumber) {
    /**
     * Creates file location with path and line number.
     * @param filePath the file path
     * @param lineNumber the line number (must be positive)
     * @throws IllegalArgumentException if lineNumber is not positive
     */
    public FileLocation(final Path filePath, final int lineNumber) {
        this.filePath = Objects.requireNonNull(filePath, "File path cannot be null");
        this.lineNumber = lineNumber;

        if (lineNumber < 1) {
            throw new IllegalArgumentException("Line number must be positive, got: " + lineNumber);
        }

    }

    /**
     * Returns the file path.
     * @return file a path
     */
    @Override
    public Path filePath() {
        return filePath;
    }

    /**
     * Returns the line number.
     * @return line number
     */
    @Override
    public int lineNumber() {
        return lineNumber;
    }

    @Override
    public String toString() {
        return String.format("FileLocation{file=%s, line=%d}", filePath, lineNumber);
    }
}
