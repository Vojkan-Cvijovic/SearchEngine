package com.demo.searchengine.service;

import java.nio.file.Path;
import java.util.List;

import com.demo.searchengine.core.SearchResult;
import com.demo.searchengine.performance.PerformanceMetrics;

/**
 * Main service interface for the text indexing library. Provides high-level operations for indexing files and
 * searching content.
 */
public interface TextIndexingService {

    /**
     * Index a single file.
     * @param filePath the file to index
     * @return true if indexing was successful
     * @throws IllegalArgumentException if filePath is null or invalid
     */
    boolean indexFile(Path filePath);

    /**
     * Index all files in a directory recursively.
     * @param directoryPath the directory to index
     * @return number of successfully indexed files
     * @throws IllegalArgumentException if directoryPath is null or invalid
     */
    int indexAllFilesInDirectory(Path directoryPath);

    /**
     * Remove a file from the index.
     * @param filePath the file to remove
     * @return true if removal was successful
     */
    boolean removeFile(Path filePath);

    /**
     * Search for single or multiple terms with AND logic (all terms must be present).
     * @param terms the terms to search for
     * @return list of search results, sorted by relevance
     */
    List<SearchResult> searchAll(List<String> terms);

    /**
     * Get performance metrics.
     * @return performance metrics
     */
    PerformanceMetrics getPerformanceMetrics();
}
