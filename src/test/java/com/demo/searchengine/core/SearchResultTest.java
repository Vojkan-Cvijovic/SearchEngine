package com.demo.searchengine.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SearchResultTest {

    @TempDir
    private Path tempDir;

    @Test
    void constructor_WithValidParameters_CreatesInstance() {
        final Path filePath = tempDir.resolve("test.txt");
        final int lineNumber = 5;
        final String matchedText = "test content";
        final long fileSize = 1024L;

        final SearchResult result = new SearchResult(filePath, lineNumber, matchedText, fileSize);

        assertNotNull(result);
        assertEquals("test.txt", result.getFileName());
        assertEquals(lineNumber, result.getLineNumber());
    }

    @Test
    void constructor_WithThreeParameters_CreatesInstanceWithZeroFileSize() {
        final Path filePath = tempDir.resolve("test.txt");
        final int lineNumber = 10;
        final String matchedText = "another test";

        final SearchResult result = new SearchResult(filePath, lineNumber, matchedText);

        assertNotNull(result);
        assertEquals("test.txt", result.getFileName());
        assertEquals(lineNumber, result.getLineNumber());
    }

    @Test
    void constructor_WithNullFilePath_ThrowsException() {
        assertThrows(NullPointerException.class, () -> new SearchResult(null, 1, "test"));
    }

    @Test
    void constructor_WithNullMatchedText_ThrowsException() {
        final Path filePath = tempDir.resolve("test.txt");
        assertThrows(NullPointerException.class, () -> new SearchResult(filePath, 1, null));
    }

    @Test
    void constructor_WithZeroLineNumber_ThrowsException() {
        final Path filePath = tempDir.resolve("test.txt");
        assertThrows(IllegalArgumentException.class, () -> new SearchResult(filePath, 0, "test"));
    }

    @Test
    void constructor_WithNegativeLineNumber_ThrowsException() {
        final Path filePath = tempDir.resolve("test.txt");
        assertThrows(IllegalArgumentException.class, () -> new SearchResult(filePath, -1, "test"));
    }

    @Test
    void constructor_WithNegativeFileSize_ThrowsException() {
        final Path filePath = tempDir.resolve("test.txt");
        assertThrows(IllegalArgumentException.class, () -> new SearchResult(filePath, 1, "test", -100L));
    }

    @Test
    void getFileName_WithNestedPath_ReturnsCorrectFileName() {
        final Path filePath = tempDir.resolve("subfolder")
                .resolve("document.txt");
        final SearchResult result = new SearchResult(filePath, 15, "content");

        assertEquals("document.txt", result.getFileName());
    }

    @Test
    void getFileName_ReturnsCorrectFileName() {
        final Path filePath = tempDir.resolve("subfolder")
                .resolve("file.md");
        final SearchResult result = new SearchResult(filePath, 20, "markdown content");

        assertEquals("file.md", result.getFileName());
    }

    @Test
    void getLineNumber_ReturnsCorrectLineNumber() {
        final SearchResult result = new SearchResult(tempDir.resolve("test.txt"), 42, "line content");
        assertEquals(42, result.getLineNumber());
    }

    @Test
    void getMatchedText_NotAvailable_FieldIsPrivate() {
        // Note: getMatchedText() method is not available in the public API
        // The matchedText field is private and only accessible through toString() and equals()
        final SearchResult result = new SearchResult(tempDir.resolve("test.txt"), 1, "test content");
        assertNotNull(result);
        // We can only verify the object was created successfully
    }

    @Test
    void getFileSize_NotAvailable_FieldIsPrivate() {
        // Note: getFileSize() method is not available in the public API
        // The fileSize field is private and only accessible through toString() and equals()
        final long expectedSize = 2048L;
        final SearchResult result = new SearchResult(tempDir.resolve("test.txt"), 1, "content", expectedSize);
        assertNotNull(result);
        // We can only verify the object was created successfully
    }

    @Test
    void compareTo_WithDifferentFileNames_ReturnsCorrectOrder() {
        final SearchResult result1 = new SearchResult(tempDir.resolve("a.txt"), 1, "content");
        final SearchResult result2 = new SearchResult(tempDir.resolve("b.txt"), 1, "content");

        assertTrue(result1.compareTo(result2) < 0);
        assertTrue(result2.compareTo(result1) > 0);
    }

    @Test
    void compareTo_WithSameFileNameDifferentLineNumbers_ReturnsCorrectOrder() {
        final SearchResult result1 = new SearchResult(tempDir.resolve("same.txt"), 5, "content");
        final SearchResult result2 = new SearchResult(tempDir.resolve("same.txt"), 10, "content");

        assertTrue(result1.compareTo(result2) < 0);
        assertTrue(result2.compareTo(result1) > 0);
    }

    @Test
    void compareTo_WithSameFileNameAndLineNumber_ReturnsZero() {
        final SearchResult result1 = new SearchResult(tempDir.resolve("same.txt"), 5, "content");
        final SearchResult result2 = new SearchResult(tempDir.resolve("same.txt"), 5, "content");

        assertEquals(0, result1.compareTo(result2));
        assertEquals(0, result2.compareTo(result1));
    }

    @Test
    void equals_WithNull_ReturnsFalse() {
        final SearchResult result = new SearchResult(tempDir.resolve("test.txt"), 1, "content");
        assertNotEquals(null, result);
    }

    @Test
    void equals_WithSameValues_ReturnsTrue() {
        final Path filePath = tempDir.resolve("test.txt");
        final SearchResult result1 = new SearchResult(filePath, 5, "content", 1024L);
        final SearchResult result2 = new SearchResult(filePath, 5, "content", 1024L);

        assertEquals(result1, result2);
        assertEquals(result2, result1);
    }

    @Test
    void equals_WithDifferentFilePath_ReturnsFalse() {
        final SearchResult result1 = new SearchResult(tempDir.resolve("file1.txt"), 5, "content");
        final SearchResult result2 = new SearchResult(tempDir.resolve("file2.txt"), 5, "content");

        assertNotEquals(result1, result2);
    }

    @Test
    void equals_WithDifferentLineNumber_ReturnsFalse() {
        final SearchResult result1 = new SearchResult(tempDir.resolve("test.txt"), 5, "content");
        final SearchResult result2 = new SearchResult(tempDir.resolve("test.txt"), 10, "content");

        assertNotEquals(result1, result2);
    }

    @Test
    void equals_WithDifferentMatchedText_ReturnsFalse() {
        final SearchResult result1 = new SearchResult(tempDir.resolve("test.txt"), 5, "content1");
        final SearchResult result2 = new SearchResult(tempDir.resolve("test.txt"), 5, "content2");

        assertNotEquals(result1, result2);
    }

    @Test
    void equals_WithDifferentFileSize_ReturnsFalse() {
        final SearchResult result1 = new SearchResult(tempDir.resolve("test.txt"), 5, "content", 1024L);
        final SearchResult result2 = new SearchResult(tempDir.resolve("test.txt"), 5, "content", 2048L);

        assertNotEquals(result1, result2);
    }

    @Test
    void hashCode_WithSameValues_ReturnsSameHashCode() {
        final Path filePath = tempDir.resolve("test.txt");
        final SearchResult result1 = new SearchResult(filePath, 5, "content", 1024L);
        final SearchResult result2 = new SearchResult(filePath, 5, "content", 1024L);

        assertEquals(result1.hashCode(), result2.hashCode());
    }

    @Test
    void hashCode_WithDifferentValues_ReturnsDifferentHashCodes() {
        final SearchResult result1 = new SearchResult(tempDir.resolve("file1.txt"), 5, "content");
        final SearchResult result2 = new SearchResult(tempDir.resolve("file2.txt"), 5, "content");

        assertNotEquals(result1.hashCode(), result2.hashCode());
    }

    @Test
    void toString_ContainsAllRelevantInformation() {
        final Path filePath = tempDir.resolve("document.txt");
        final SearchResult result = new SearchResult(filePath, 42, "sample text", 1024L);
        final String resultString = result.toString();

        assertTrue(resultString.contains("SearchResult"));
        assertTrue(resultString.contains("document.txt"));
        assertTrue(resultString.contains("42"));
        assertTrue(resultString.contains("sample text"));
        assertTrue(resultString.contains("1024"));
    }

    @Test
    void toString_WithZeroFileSize_ShowsZero() {
        final SearchResult result = new SearchResult(tempDir.resolve("test.txt"), 1, "content");
        final String resultString = result.toString();

        assertTrue(resultString.contains("size=0"));
    }

    @Test
    void constructor_WithSpecialCharactersInFileName_HandlesCorrectly() {
        final Path filePath = tempDir.resolve("file with spaces.txt");
        final SearchResult result = new SearchResult(filePath, 1, "content");

        assertEquals("file with spaces.txt", result.getFileName());
    }

    @Test
    void constructor_WithLongMatchedText_HandlesCorrectly() {
        final String longText =
            "This is a very long matched text that contains many characters and should be handled properly by the SearchResult class without any issues";
        final SearchResult result = new SearchResult(tempDir.resolve("test.txt"), 1, longText);

        assertNotNull(result);
        // We can only verify the object was created successfully since getMatchedText() is not public
    }

    @Test
    void constructor_WithLargeLineNumber_HandlesCorrectly() {
        final int largeLineNumber = Integer.MAX_VALUE;
        final SearchResult result = new SearchResult(tempDir.resolve("test.txt"), largeLineNumber, "content");

        assertEquals(largeLineNumber, result.getLineNumber());
    }

    @Test
    void constructor_WithLargeFileSize_HandlesCorrectly() {
        final long largeFileSize = Long.MAX_VALUE;
        final SearchResult result = new SearchResult(tempDir.resolve("test.txt"), 1, "content", largeFileSize);

        assertNotNull(result);
        // We can only verify the object was created successfully since getFileSize() is not public
    }
}
