package com.demo.searchengine.core.impl;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.demo.searchengine.core.FileLocation;
import com.demo.searchengine.core.FileMetadata;
import com.demo.searchengine.core.Index;
import com.demo.searchengine.tokenizer.model.Token;

/**
 * Thread-safe in-memory inverted index implementation. Uses read-write locks only for critical sections where data
 * structures are updated. Maps terms to their locations in files for fast search operations. Thread Safety
 * Guarantees: - Multiple readers can access the index concurrently - Writers have exclusive access only during
 * critical data updates - Input validation happens outside locks for better performance - All compound operations are
 * atomic - No race conditions between related operations
 */
public class ThreadSafeIndex implements Index {
    private static final Logger logger = LogManager.getLogger(ThreadSafeIndex.class);

    // Read-write lock for protecting critical data structure updates
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    // Inverted index: term -> set of file locations
    private final Map<String, Set<FileLocation>> termToLocations;

    // File metadata: path -> file information
    private final Map<Path, FileMetadata> fileMetadata;

    // File to terms mapping for efficient updates and removals
    private final Map<Path, Set<String>> fileToTerms;

    // Statistics - using AtomicInteger for simple counters
    private final AtomicInteger totalTerms;
    private final AtomicInteger totalFiles;

    public ThreadSafeIndex() {
        this.termToLocations = new HashMap<>();
        this.fileMetadata = new HashMap<>();
        this.fileToTerms = new HashMap<>();
        this.totalTerms = new AtomicInteger(0);
        this.totalFiles = new AtomicInteger(0);
    }

    // ==================== INDEXING OPERATIONS ====================

    @Override
    public void addTermsWithLineInfo(final List<Token> tokens, final Path filePath) {
        // Validate input outside the lock
        if (filePath == null) {
            throw new IllegalArgumentException("File path cannot be null");
        }
        if (tokens == null || tokens.isEmpty()) {
            logger.info("No tokens to add for file: {}", filePath);
            return;
        }

        // Pre-process and validate tokens outside the lock
        final List<Token> validTokens = filterValidTokens(tokens);
        if (validTokens.isEmpty()) {
            logger.info("No valid tokens found for file: {}", filePath);
            return;
        }

        // Only lock during the critical section where we update data structures
        lock.writeLock()
                .lock();
        try {
            // Check if this is a new file (for statistics)
            final boolean isNewFile = !fileToTerms.containsKey(filePath);

            // Add all valid tokens
            for (final Token token : validTokens) {
                addTermToIndexInternal(token.getValue(), new FileLocation(filePath, token.getLineNumber()));
            }

            // Update file count if this is a new file
            if (isNewFile) {
                totalFiles.incrementAndGet();
            }

            logger.debug("Added {} terms for file: {}", validTokens.size(), filePath);
        } finally {
            lock.writeLock()
                    .unlock();
        }
    }

    @Override
    public void updateTermsForFile(final List<Token> tokens, final Path filePath) {
        // Validate input outside the lock
        if (filePath == null) {
            throw new IllegalArgumentException("File path cannot be null");
        }

        // Pre-process tokens outside the lock
        final List<Token> validTokens = filterValidTokens(tokens);

        // Only lock during the critical section
        lock.writeLock()
                .lock();
        try {
            logger.info("Updating terms for file: {}", filePath);

            // Remove all existing locations for this file
            final int removedCount = removeFileLocationsInternal(filePath);

            // Add new terms with line information
            for (final Token tokenInfo : validTokens) {
                addTermToIndexInternal(tokenInfo.getValue(), new FileLocation(filePath, tokenInfo.getLineNumber()));
            }

            logger.debug("Updated file: {} - removed {} old terms, added {} new terms", filePath, removedCount,
                validTokens.size());
        } finally {
            lock.writeLock()
                    .unlock();
        }
    }

    @Override
    public void addFileMetadata(final FileMetadata metadata) {
        // Validate input outside the lock
        if (metadata == null) {
            throw new IllegalArgumentException("File metadata cannot be null");
        }

        // Only lock during the critical section
        lock.writeLock()
                .lock();
        try {
            fileMetadata.put(metadata.getFilePath(), metadata);
            logger.info("Added file metadata for: {}", metadata.getFileName());
        } finally {
            lock.writeLock()
                    .unlock();
        }
    }

    @Override
    public void removeFile(final Path filePath) {
        // Validate input outside the lock
        if (filePath == null) {
            throw new IllegalArgumentException("File path cannot be null");
        }

        // Only lock during the critical section
        lock.writeLock()
                .lock();
        try {
            logger.info("Removing file from index: {}", filePath);

            // Remove file metadata
            final FileMetadata removedMetadata = fileMetadata.remove(filePath);
            if (removedMetadata != null) {
                logger.info("Removed metadata for file: {}", filePath);
            }

            // Remove all locations for this file
            final int removedCount = removeFileLocationsInternal(filePath);

            // Update file count
            if (removedCount > 0 || removedMetadata != null) {
                totalFiles.decrementAndGet();
            }

            logger.info("Removed file from index: {} (removed {} terms)", filePath, removedCount);
        } finally {
            lock.writeLock()
                    .unlock();
        }
    }

    @Override
    public void clear() {
        // Only lock during the critical section
        lock.writeLock()
                .lock();
        try {
            logger.info("Clearing index");
            termToLocations.clear();
            fileMetadata.clear();
            fileToTerms.clear();
            totalTerms.set(0);
            totalFiles.set(0);
            logger.info("Index cleared");
        } finally {
            lock.writeLock()
                    .unlock();
        }
    }

    @Override
    public Set<FileLocation> findAllTerms(final List<String> terms) {
        // Validate input outside the lock
        if (terms == null) {
            throw new IllegalArgumentException("Terms list cannot be null");
        }
        if (terms.isEmpty()) {
            return Set.of();
        }

        // Pre-process terms outside the lock
        final List<String> validTerms = filterValidTerms(terms);
        if (validTerms.isEmpty()) {
            return Set.of();
        }

        // Only lock during the critical section
        lock.readLock()
                .lock();
        try {
            Set<FileLocation> result = null;

            for (final String term : validTerms) {
                final String normalizedTerm = normalizeTerm(term);
                final Set<FileLocation> termLocations = termToLocations.get(normalizedTerm);

                if (termLocations == null || termLocations.isEmpty()) {
                    // If any term is not found, a result is empty
                    return Set.of();
                }

                if (result == null) {
                    result = new HashSet<>(termLocations);
                } else {
                    result.retainAll(termLocations);
                    if (result.isEmpty()) {
                        // No common locations
                        return Set.of();
                    }
                }
            }

            return result != null ? Collections.unmodifiableSet(result) : Set.of();
        } finally {
            lock.readLock()
                    .unlock();
        }
    }

    @Override
    public FileMetadata getFileMetadata(final Path filePath) {
        if (filePath == null) {
            return null;
        }

        // Only lock during the critical section
        lock.readLock()
                .lock();
        try {
            return fileMetadata.get(filePath);
        } finally {
            lock.readLock()
                    .unlock();
        }
    }

    // ==================== STATISTICS AND MONITORING ====================

    @Override
    public int getTotalTerms() {
        return totalTerms.get();
    }

    @Override
    public int getUniqueTerms() {
        // Only lock during the critical section
        lock.readLock()
                .lock();
        try {
            return termToLocations.size();
        } finally {
            lock.readLock()
                    .unlock();
        }
    }

    @Override
    public int getFileCount() {
        return totalFiles.get();
    }

    @Override
    public boolean isEmpty() {
        // Only lock during the critical section
        lock.readLock()
                .lock();
        try {
            return termToLocations.isEmpty();
        } finally {
            lock.readLock()
                    .unlock();
        }
    }

    @Override
    public String getStats() {
        // Only lock during the critical section
        lock.readLock()
                .lock();
        try {
            return String.format("ThreadSafeIndex{files=%d, totalTerms=%d, uniqueTerms=%d, isEmpty=%s}",
                getFileCount(), getTotalTerms(), getUniqueTerms(), isEmpty());
        } finally {
            lock.readLock()
                    .unlock();
        }
    }

    // ==================== PRIVATE HELPER METHODS ====================

    /**
     * Internal method to add a term to the index (assumes write lock is held).
     */
    void addTermToIndexInternal(final String term, final FileLocation location) {
        final String normalizedTerm = normalizeTerm(term);
        if (normalizedTerm.isEmpty()) {
            return;
        }

        // Add location to term's set
        termToLocations.computeIfAbsent(normalizedTerm, k -> new HashSet<>())
                .add(location);

        // Add term to file's set
        fileToTerms.computeIfAbsent(location.filePath(), k -> new HashSet<>())
                .add(normalizedTerm);

        totalTerms.incrementAndGet();

        logger.debug("Added term '{}' to index at location {}", normalizedTerm, location);
    }

    /**
     * Internal method to remove all locations for a specific file (assumes write lock is held).
     * @param filePath the file whose locations should be removed
     * @return number of terms removed
     */
    int removeFileLocationsInternal(final Path filePath) {
        final Set<String> fileTerms = fileToTerms.get(filePath);
        if (fileTerms == null || fileTerms.isEmpty()) {
            return 0;
        }

        int removedCount = 0;

        // Remove all locations for this file from term mappings
        for (final String term : fileTerms) {
            final Set<FileLocation> locations = termToLocations.get(term);
            if (locations != null) {
                final int beforeSize = locations.size();
                locations.removeIf(location -> location.filePath()
                        .equals(filePath));
                removedCount += (beforeSize - locations.size());

                // If term's set is empty, remove the term entirely
                if (locations.isEmpty()) {
                    termToLocations.remove(term);
                }
            }
        }

        // Remove from file-to-terms mapping
        fileToTerms.remove(filePath);

        // Update total terms count
        totalTerms.addAndGet(-removedCount);

        logger.info("Removed {} locations for file: {}", removedCount, filePath);
        return removedCount;
    }

    /**
     * Filter and validate tokens outside the lock for better performance.
     */
    List<Token> filterValidTokens(final List<Token> tokenInfos) {
        final List<Token> validTokens = new ArrayList<>();
        for (final Token tokenInfo : tokenInfos) {
            if (isValidToken(tokenInfo)) {
                validTokens.add(tokenInfo);
            }
        }
        return validTokens;
    }

    /**
     * Filter and validate terms outside the lock for better performance.
     */
    List<String> filterValidTerms(final List<String> terms) {
        final List<String> validTerms = new ArrayList<>();
        for (final String term : terms) {
            if (term != null && !term.trim()
                    .isEmpty()) {
                validTerms.add(term);
            }
        }
        return validTerms;
    }

    /**
     * Normalize a term for consistent indexing.
     */
    String normalizeTerm(final String term) {
        return term.toLowerCase()
                .trim();
    }

    /**
     * Check if a token is valid for indexing.
     */
    boolean isValidToken(final Token tokenInfo) {
        return tokenInfo != null && tokenInfo.getValue() != null && !tokenInfo.getValue()
                .trim()
                .isEmpty();
    }

    @Override
    public String toString() {
        return getStats();
    }
}
