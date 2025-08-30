package com.demo.searchengine.performance;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Performance metrics for the text indexing library. Tracks key performance indicators for monitoring and
 * optimization.
 */
public class PerformanceMetrics {
    // Counters
    private final AtomicLong totalFilesIndexed = new AtomicLong(0);
    private final AtomicLong totalSearchQueries = new AtomicLong(0);
    private final AtomicLong totalIndexingTime = new AtomicLong(0);
    private final AtomicLong totalSearchTime = new AtomicLong(0);

    // Memory metrics
    private final AtomicLong peakMemoryUsage = new AtomicLong(0);

    // Performance thresholds
    private static final long SLOW_INDEXING_THRESHOLD_MS = 1000; // 1 second
    private static final long SLOW_SEARCH_THRESHOLD_MS = 100;    // 100ms

    /**
     * Records file indexing operation with duration.
     * @param indexingTimeMs indexing duration in milliseconds
     */
    public void recordFileIndexed(final long indexingTimeMs) {
        totalFilesIndexed.incrementAndGet();
        totalIndexingTime.addAndGet(indexingTimeMs);

        // Track peak memory usage
        updateMemoryUsage();

    }

    /**
     * Records search query operation with duration.
     * @param searchTimeMs search duration in milliseconds
     */
    public void recordSearchQuery(final long searchTimeMs) {
        totalSearchQueries.incrementAndGet();
        totalSearchTime.addAndGet(searchTimeMs);

    }

    private void updateMemoryUsage() {
        final Runtime runtime = Runtime.getRuntime();
        final long currentMemory = runtime.totalMemory() - runtime.freeMemory();

        // Update peak memory if current usage is higher
        long currentPeak = peakMemoryUsage.get();
        while (currentMemory > currentPeak && !peakMemoryUsage.compareAndSet(currentPeak, currentMemory)) {
            currentPeak = peakMemoryUsage.get();
        }
    }

    // Getters for metrics

    /**
     * Returns total number of files indexed.
     * @return total files indexed
     */
    public long getTotalFilesIndexed() {
        return totalFilesIndexed.get();
    }

    /**
     * Returns total number of search queries.
     * @return total search queries
     */
    public long getTotalSearchQueries() {
        return totalSearchQueries.get();
    }

    /**
     * Returns average indexing time.
     * @return average indexing duration
     */
    public Duration getAverageIndexingTime() {
        final long total = totalFilesIndexed.get();
        if (total == 0)
            return Duration.ZERO;

        final long avgMs = totalIndexingTime.get() / total;
        return Duration.ofMillis(avgMs);
    }

    /**
     * Returns average search time.
     * @return average search duration
     */
    public Duration getAverageSearchTime() {
        final long total = totalSearchQueries.get();
        if (total == 0)
            return Duration.ZERO;

        final long avgMs = totalSearchTime.get() / total;
        return Duration.ofMillis(avgMs);
    }

    /**
     * Returns peak memory usage in bytes.
     * @return peak memory usage
     */
    public long getPeakMemoryUsage() {
        updateMemoryUsage(); // Ensure we have current data
        return peakMemoryUsage.get();
    }

    /**
     * Returns current memory usage in bytes.
     * @return current memory usage
     */
    public long getCurrentMemoryUsage() {
        final Runtime runtime = Runtime.getRuntime();
        return runtime.totalMemory() - runtime.freeMemory();
    }

    /**
     * Returns maximum available memory in bytes.
     * @return maximum memory
     */
    public long getMaxMemory() {
        return Runtime.getRuntime()
                .maxMemory();
    }

    // Performance health indicators

    /**
     * Checks if system performance is healthy.
     * @return true if performance is within thresholds
     */
    public boolean isHealthy() {
        return getAverageSearchTime().toMillis() < SLOW_SEARCH_THRESHOLD_MS
                && getAverageIndexingTime().toMillis() < SLOW_INDEXING_THRESHOLD_MS;
    }

    /**
     * Returns health status summary.
     * @return health summary string
     */
    public String getHealthSummary() {
        if (isHealthy()) {
            return "System is performing well";
        }

        final StringBuilder summary = new StringBuilder("Performance issues detected: ");
        boolean hasIssues = false;

        if (getAverageSearchTime().toMillis() >= SLOW_SEARCH_THRESHOLD_MS) {
            summary.append("Slow search performance (")
                    .append(getAverageSearchTime().toMillis())
                    .append("ms avg)");
            hasIssues = true;
        }

        if (getAverageIndexingTime().toMillis() >= SLOW_INDEXING_THRESHOLD_MS) {
            if (hasIssues)
                summary.append(", ");
            summary.append("Slow indexing performance (")
                    .append(getAverageIndexingTime().toMillis())
                    .append("ms avg)");
        }

        return summary.toString();
    }

    // Reset metrics (useful for testing or periodic resets)
    /**
     * Resets all metrics to zero.
     */
    void reset() {
        totalFilesIndexed.set(0);
        totalSearchQueries.set(0);
        totalIndexingTime.set(0);
        totalSearchTime.set(0);

        peakMemoryUsage.set(0);
    }

    @Override
    public String toString() {
        return String.format(
            "PerformanceMetrics{filesIndexed=%d, searchQueries=%d, " + "avgIndexingTime=%dms, avgSearchTime=%dms, "
                    + "peakMemory=%d bytes, healthy=%s}",
            getTotalFilesIndexed(), getTotalSearchQueries(), getAverageIndexingTime().toMillis(),
            getAverageSearchTime().toMillis(), getPeakMemoryUsage(), isHealthy());
    }
}
