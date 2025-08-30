package com.demo.searchengine.performance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PerformanceMetricsTest {

    private PerformanceMetrics metrics;

    @BeforeEach
    void setUp() {
        metrics = new PerformanceMetrics();
    }

    @Test
    void recordFileIndexed_WithValidData_RecordsCorrectly() {
        metrics.recordFileIndexed(100);

        assertEquals(1, metrics.getTotalFilesIndexed());
        assertEquals(Duration.ofMillis(100), metrics.getAverageIndexingTime());
    }

    @Test
    void recordFileIndexed_WithMultipleRecords_CalculatesStatisticsCorrectly() {
        metrics.recordFileIndexed(100);
        metrics.recordFileIndexed(200);
        metrics.recordFileIndexed(300);

        assertEquals(3, metrics.getTotalFilesIndexed());
        assertEquals(Duration.ofMillis(200), metrics.getAverageIndexingTime());
    }

    @Test
    void recordSearchQuery_WithValidData_RecordsCorrectly() {
        metrics.recordSearchQuery(50);

        assertEquals(1, metrics.getTotalSearchQueries());
        assertEquals(Duration.ofMillis(50), metrics.getAverageSearchTime());
    }

    @Test
    void recordSearchQuery_WithMultipleRecords_CalculatesStatisticsCorrectly() {
        metrics.recordSearchQuery(50);
        metrics.recordSearchQuery(100);
        metrics.recordSearchQuery(150);

        assertEquals(3, metrics.getTotalSearchQueries());
        assertEquals(Duration.ofMillis(100), metrics.getAverageSearchTime());
    }

    @Test
    void recordFileIndexed_WithZeroTime_RecordsCorrectly() {
        metrics.recordFileIndexed(0);

        assertEquals(1, metrics.getTotalFilesIndexed());
        assertEquals(Duration.ofMillis(0), metrics.getAverageIndexingTime());
    }

    @Test
    void recordSearchQuery_WithZeroTime_RecordsCorrectly() {
        metrics.recordSearchQuery(0);

        assertEquals(1, metrics.getTotalSearchQueries());
        assertEquals(Duration.ofMillis(0), metrics.getAverageSearchTime());
    }

    @Test
    void getTotalFilesIndexed_WithNoRecords_ReturnsZero() {
        assertEquals(0, metrics.getTotalFilesIndexed());
    }

    @Test
    void getTotalSearchQueries_WithNoRecords_ReturnsZero() {
        assertEquals(0, metrics.getTotalSearchQueries());
    }

    @Test
    void getAverageIndexingTime_WithNoRecords_ReturnsZero() {
        assertEquals(Duration.ZERO, metrics.getAverageIndexingTime());
    }

    @Test
    void getAverageSearchTime_WithNoRecords_ReturnsZero() {
        assertEquals(Duration.ZERO, metrics.getAverageSearchTime());
    }

    @Test
    void getPeakMemoryUsage_ReturnsCurrentPeak() {
        final long peakMemory = metrics.getPeakMemoryUsage();
        assertTrue(peakMemory >= 0);
    }

    @Test
    void getCurrentMemoryUsage_ReturnsCurrentUsage() {
        final long currentMemory = metrics.getCurrentMemoryUsage();
        assertTrue(currentMemory >= 0);
    }

    @Test
    void getMaxMemory_ReturnsMaxMemory() {
        final long maxMemory = metrics.getMaxMemory();
        assertTrue(maxMemory > 0);
    }

    @Test
    void isHealthy_WithFastOperations_ReturnsTrue() {
        metrics.recordFileIndexed(500);
        metrics.recordSearchQuery(50);

        assertTrue(metrics.isHealthy());
    }

    @Test
    void isHealthy_WithSlowIndexing_ReturnsFalse() {
        metrics.recordFileIndexed(1500);
        metrics.recordSearchQuery(50);

        assertFalse(metrics.isHealthy());
    }

    @Test
    void isHealthy_WithSlowSearch_ReturnsFalse() {
        metrics.recordFileIndexed(500);
        metrics.recordSearchQuery(150);

        assertFalse(metrics.isHealthy());
    }

    @Test
    void isHealthy_WithSlowOperations_ReturnsFalse() {
        metrics.recordFileIndexed(1500);
        metrics.recordSearchQuery(150);

        assertFalse(metrics.isHealthy());
    }

    @Test
    void getHealthSummary_WithHealthySystem_ReturnsPositiveMessage() {
        metrics.recordFileIndexed(500);
        metrics.recordSearchQuery(50);

        final String summary = metrics.getHealthSummary();
        assertTrue(summary.contains("System is performing well"));
    }

    @Test
    void getHealthSummary_WithSlowIndexing_ReturnsWarningMessage() {
        metrics.recordFileIndexed(1500);
        metrics.recordSearchQuery(50);

        final String summary = metrics.getHealthSummary();
        assertTrue(summary.contains("Performance issues detected"));
        assertTrue(summary.contains("Slow indexing performance"));
        assertTrue(summary.contains("1500ms avg"));
    }

    @Test
    void getHealthSummary_WithSlowSearch_ReturnsWarningMessage() {
        metrics.recordFileIndexed(500);
        metrics.recordSearchQuery(150);

        final String summary = metrics.getHealthSummary();
        assertTrue(summary.contains("Performance issues detected"));
        assertTrue(summary.contains("Slow search performance"));
        assertTrue(summary.contains("150ms avg"));
    }

    @Test
    void reset_WithPopulatedMetrics_ClearsAllData() {
        metrics.recordFileIndexed(100);
        metrics.recordSearchQuery(50);

        assertEquals(1, metrics.getTotalFilesIndexed());
        assertEquals(1, metrics.getTotalSearchQueries());

        metrics.reset();

        assertEquals(0, metrics.getTotalFilesIndexed());
        assertEquals(0, metrics.getTotalSearchQueries());
        assertEquals(Duration.ZERO, metrics.getAverageIndexingTime());
        assertEquals(Duration.ZERO, metrics.getAverageSearchTime());
    }

    @Test
    void toString_ContainsAllMetrics() {
        metrics.recordFileIndexed(100);
        metrics.recordSearchQuery(50);

        final String result = metrics.toString();
        assertTrue(result.contains("PerformanceMetrics"));
        assertTrue(result.contains("filesIndexed=1"));
        assertTrue(result.contains("searchQueries=1"));
        assertTrue(result.contains("avgIndexingTime=100ms"));
        assertTrue(result.contains("avgSearchTime=50ms"));
        assertTrue(result.contains("healthy=true"));
    }

    @Test
    void recordFileIndexed_WithNegativeTime_RecordsCorrectly() {
        metrics.recordFileIndexed(-100);

        assertEquals(1, metrics.getTotalFilesIndexed());
        assertEquals(Duration.ofMillis(-100), metrics.getAverageIndexingTime());
    }

    @Test
    void recordSearchQuery_WithNegativeTime_RecordsCorrectly() {
        metrics.recordSearchQuery(-50);

        assertEquals(1, metrics.getTotalSearchQueries());
        assertEquals(Duration.ofMillis(-50), metrics.getAverageSearchTime());
    }

    @Test
    void memoryUsage_TracksPeakCorrectly() {
        final long initialPeak = metrics.getPeakMemoryUsage();

        metrics.recordFileIndexed(100);
        metrics.recordSearchQuery(50);

        final long currentPeak = metrics.getPeakMemoryUsage();
        assertTrue(currentPeak >= initialPeak);
    }
}
