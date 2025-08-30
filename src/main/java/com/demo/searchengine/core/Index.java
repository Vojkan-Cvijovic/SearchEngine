package com.demo.searchengine.core;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import com.demo.searchengine.tokenizer.model.Token;

/**
 * Interface for search engine index implementations. Defines the contract for indexing and searching operations.
 * Implementations must provide: - Thread-safe operations (if required by the implementation) - Efficient term
 * indexing and lookup - File metadata management - Statistics and monitoring capabilities
 */
public interface Index {

    // ==================== INDEXING OPERATIONS ====================

    /**
     * Add multiple terms with line information from a file to the index.
     * @param tokens list of tokens with line information
     * @param filePath the file containing the terms
     * @throws IllegalArgumentException if filePath is null
     */
    void addTermsWithLineInfo(List<Token> tokens, Path filePath);

    /**
     * Update terms for a file by replacing all existing locations with new ones. This operation should be atomic -
     * the file is either fully updated or not at all.
     * @param tokens list of tokens with line information
     * @param filePath the file containing the terms
     * @throws IllegalArgumentException if filePath is null
     */
    void updateTermsForFile(List<Token> tokens, Path filePath);

    /**
     * Add file metadata to the index.
     * @param metadata the file metadata
     * @throws IllegalArgumentException if metadata is null
     */
    void addFileMetadata(FileMetadata metadata);

    /**
     * Remove a file completely from the index. This operation should be atomic.
     * @param filePath the file to remove
     * @throws IllegalArgumentException if filePath is null
     */
    void removeFile(Path filePath);

    /**
     * Clear all data from the index. This operation should be atomic.
     */
    void clear();

    // ==================== SEARCH OPERATIONS ====================

    /**
     * Find all locations of multiple terms (AND search). All terms must appear in the same files.
     * @param terms list of terms to search for
     * @return set of file locations where all terms appear
     * @throws IllegalArgumentException if terms is null
     */
    Set<FileLocation> findAllTerms(List<String> terms);

    /**
     * Get metadata for a specific file.
     * @param filePath the file path
     * @return file metadata, or null if not found
     */
    FileMetadata getFileMetadata(Path filePath);

    // ==================== STATISTICS AND MONITORING ====================

    /**
     * Get the total number of term occurrences in the index.
     * @return total term count
     */
    int getTotalTerms();

    /**
     * Get the number of unique terms in the index.
     * @return unique term count
     */
    int getUniqueTerms();

    /**
     * Get the number of indexed files.
     * @return file count
     */
    int getFileCount();

    /**
     * Check if the index is empty.
     * @return true if no terms are indexed
     */
    boolean isEmpty();

    /**
     * Get comprehensive index statistics.
     * @return formatted statistics string
     */
    String getStats();
}
