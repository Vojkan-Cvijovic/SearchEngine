package com.demo.searchengine.core.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import com.demo.searchengine.core.FileLocation;
import com.demo.searchengine.core.FileMetadata;
import com.demo.searchengine.tokenizer.model.Token;
import com.demo.searchengine.tokenizer.model.TokenInfo;

@ExtendWith(MockitoExtension.class)
class ThreadSafeIndexTest {

    private ThreadSafeIndex index;
    private Path testFilePath;
    private List<Token> testTokens;

    @BeforeEach
    void setUp() {
        index = new ThreadSafeIndex();
        testFilePath = Paths.get("/test/file.txt");
        testTokens = Arrays.asList(new TokenInfo("hello", 1), new TokenInfo("world", 2), new TokenInfo("test", 3));
    }

    @Test
    void addTermsWithLineInfo_WithValidTokens_AddsTokensToIndex() {
        index.addTermsWithLineInfo(testTokens, testFilePath);

        final Set<FileLocation> helloLocations = index.findAllTerms(List.of("hello"));
        final Set<FileLocation> worldLocations = index.findAllTerms(List.of("world"));

        assertEquals(1, helloLocations.size());
        assertEquals(1, worldLocations.size());
        assertTrue(helloLocations.stream()
                .anyMatch(loc -> loc.filePath()
                        .equals(testFilePath)));
        assertTrue(worldLocations.stream()
                .anyMatch(loc -> loc.filePath()
                        .equals(testFilePath)));
    }

    @Test
    void addTermsWithLineInfo_WithNullFilePath_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> index.addTermsWithLineInfo(testTokens, null));
    }

    @Test
    void addTermsWithLineInfo_WithNullTokens_LogsInfoAndReturns() {
        index.addTermsWithLineInfo(null, testFilePath);

        // Should not throw exception and should not add anything
        assertTrue(index.isEmpty());
    }

    @Test
    void addTermsWithLineInfo_WithEmptyTokens_LogsInfoAndReturns() {
        index.addTermsWithLineInfo(List.of(), testFilePath);

        // Should not throw exception and should not add anything
        assertTrue(index.isEmpty());
    }

    @Test
    void updateTermsForFile_WithNullFilePath_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> index.updateTermsForFile(testTokens, null));
    }

    @Test
    void findTerm_WithExistingTerm_ReturnsLocations() {
        index.addTermsWithLineInfo(testTokens, testFilePath);

        final Set<FileLocation> locations = index.findAllTerms(List.of("hello"));
        assertEquals(1, locations.size());
        assertTrue(locations.stream()
                .anyMatch(loc -> loc.filePath()
                        .equals(testFilePath)));
    }

    @Test
    void findAllTerms_WithAllTermsPresent_ReturnsCommonLocations() {
        index.addTermsWithLineInfo(testTokens, testFilePath);

        final List<String> searchTerms = Arrays.asList("hello", "world");
        final Set<FileLocation> locations = index.findAllTerms(searchTerms);

        // Since "hello" is on line 1 and "world" is on line 2, there are no common locations
        // findAllTerms looks for FileLocation objects that are exactly equal (same file AND same line)
        assertEquals(0, locations.size());
    }

    @Test
    void findAllTerms_WithTermsOnSameLine_ReturnsCommonLocations() {
        // Create tokens on the same line
        final List<Token> sameLineTokens = Arrays.asList(new TokenInfo("hello", 1), new TokenInfo("world", 1)  // Same
                                                                                                               // line
                                                                                                               // as
                                                                                                               // hello
        );
        index.addTermsWithLineInfo(sameLineTokens, testFilePath);

        final Set<FileLocation> locations = index.findAllTerms(Arrays.asList("hello", "world"));
        // Now both terms are on the same line, so there should be 1 common location
        assertEquals(1, locations.size());
        assertTrue(locations.stream()
                .anyMatch(loc -> loc.filePath()
                        .equals(testFilePath)));
    }

    @Test
    void findAllTerms_WithSomeTermsMissing_ReturnsEmptySet() {
        index.addTermsWithLineInfo(testTokens, testFilePath);

        final Set<FileLocation> locations = index.findAllTerms(Arrays.asList("hello", "nonexistent"));
        assertTrue(locations.isEmpty());
    }

    @Test
    void findAllTerms_WithNullTerms_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> index.findAllTerms(null));
    }

    @Test
    void addFileMetadata_WithValidMetadata_AddsMetadata() {
        final FileMetadata metadata = new FileMetadata(testFilePath, 1024L, java.time.Instant.now());
        index.addFileMetadata(metadata);

        final FileMetadata retrieved = index.getFileMetadata(testFilePath);
        assertEquals(metadata, retrieved);
    }

    @Test
    void addFileMetadata_WithNullMetadata_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> index.addFileMetadata(null));
    }

    @Test
    void removeFile_WithExistingFile_RemovesFileAndTerms() {
        index.addTermsWithLineInfo(testTokens, testFilePath);
        final FileMetadata metadata = new FileMetadata(testFilePath, 1024L, java.time.Instant.now());
        index.addFileMetadata(metadata);

        assertEquals(3, index.getTotalTerms());
        assertEquals(1, index.getFileCount());

        index.removeFile(testFilePath);

        assertTrue(index.findAllTerms(List.of("hello"))
                .isEmpty());
        assertTrue(index.findAllTerms(List.of("world"))
                .isEmpty());
        assertNull(index.getFileMetadata(testFilePath));
        assertEquals(0, index.getTotalTerms());
        assertEquals(0, index.getFileCount());
    }

    @Test
    void removeFile_WithNullFilePath_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> index.removeFile(null));
    }

    @Test
    void clear_WithPopulatedIndex_ClearsAllData() {
        index.addTermsWithLineInfo(testTokens, testFilePath);
        final FileMetadata metadata = new FileMetadata(testFilePath, 1024L, java.time.Instant.now());
        index.addFileMetadata(metadata);

        assertFalse(index.isEmpty());
        assertEquals(3, index.getTotalTerms());
        assertEquals(1, index.getFileCount());

        index.clear();

        assertTrue(index.isEmpty());
        assertEquals(0, index.getTotalTerms());
        assertEquals(0, index.getFileCount());
    }

    @Test
    void getTotalTerms_WithAddedTerms_ReturnsCorrectCount() {
        index.addTermsWithLineInfo(testTokens, testFilePath);
        assertEquals(3, index.getTotalTerms());
    }

    @Test
    void getUniqueTerms_WithAddedTerms_ReturnsCorrectCount() {
        index.addTermsWithLineInfo(testTokens, testFilePath);
        assertEquals(3, index.getUniqueTerms());
    }

    @Test
    void getFileCount_WithAddedFiles_ReturnsCorrectCount() {
        index.addTermsWithLineInfo(testTokens, testFilePath);
        assertEquals(1, index.getFileCount());
    }

    @Test
    void isEmpty_WithEmptyIndex_ReturnsTrue() {
        assertTrue(index.isEmpty());
    }

    @Test
    void isEmpty_WithPopulatedIndex_ReturnsFalse() {
        index.addTermsWithLineInfo(testTokens, testFilePath);
        assertFalse(index.isEmpty());
    }

    @Test
    void getStats_WithPopulatedIndex_ReturnsFormattedString() {
        index.addTermsWithLineInfo(testTokens, testFilePath);

        final String stats = index.getStats();
        assertTrue(stats.contains("ThreadSafeIndex"));
        assertTrue(stats.contains("files=1"));
        assertTrue(stats.contains("totalTerms=3"));
        assertTrue(stats.contains("uniqueTerms=3"));
        assertTrue(stats.contains("isEmpty=false"));
    }

    @Test
    void getStats_WithEmptyIndex_ReturnsFormattedString() {
        final String stats = index.getStats();
        assertTrue(stats.contains("ThreadSafeIndex"));
        assertTrue(stats.contains("files=0"));
        assertTrue(stats.contains("totalTerms=0"));
        assertTrue(stats.contains("uniqueTerms=0"));
        assertTrue(stats.contains("isEmpty=true"));
    }

    @Test
    void toString_ReturnsStats() {
        final String stats = index.getStats();
        final String toString = index.toString();
        assertEquals(stats, toString);
    }

    // Tests for newly accessible helper methods
    @Test
    void addTermToIndexInternal_WithValidTerm_AddsToIndex() {
        final FileLocation location = new FileLocation(testFilePath, 1);
        index.addTermToIndexInternal("tester", location);

        final Set<FileLocation> locations = index.findAllTerms(List.of("tester"));
        assertEquals(1, locations.size());
        assertTrue(locations.contains(location));
    }

    @Test
    void removeFileLocationsInternal_WithExistingFile_RemovesAllLocations() {
        // Add terms first
        index.addTermsWithLineInfo(testTokens, testFilePath);
        assertEquals(3, index.getTotalTerms());

        // Remove file locations
        final int removedCount = index.removeFileLocationsInternal(testFilePath);
        assertEquals(3, removedCount);
        assertEquals(0, index.getTotalTerms());
    }

    @Test
    void filterValidTokens_WithMixedTokens_FiltersCorrectly() {
        final List<Token> mixedTokens = Arrays.asList(new TokenInfo("valid", 1), new TokenInfo("", 2),      // Empty
                                                                                                            // token
            new TokenInfo(null, 3),    // Null token
            new TokenInfo("also_valid", 4));

        final List<Token> filtered = index.filterValidTokens(mixedTokens);
        assertEquals(2, filtered.size());
        assertEquals("valid", filtered.get(0)
                .getValue());
        assertEquals("also_valid", filtered.get(1)
                .getValue());
    }

    @Test
    void filterValidTerms_WithMixedTerms_FiltersCorrectly() {
        final List<String> mixedTerms = Arrays.asList("valid", "", null, "also_valid");

        final List<String> filtered = index.filterValidTerms(mixedTerms);
        assertEquals(2, filtered.size());
        assertEquals("valid", filtered.get(0));
        assertEquals("also_valid", filtered.get(1));
    }

    @Test
    void normalizeTerm_WithVariousInputs_NormalizesCorrectly() {
        assertEquals("hello", index.normalizeTerm("  Hello  "));
        assertEquals("world", index.normalizeTerm("WORLD"));
        assertEquals("test", index.normalizeTerm("Test"));
        assertEquals("", index.normalizeTerm("   "));
    }

    @Test
    void isValidToken_WithVariousTokens_ValidatesCorrectly() {
        assertTrue(index.isValidToken(new TokenInfo("valid", 1)));
        assertFalse(index.isValidToken(new TokenInfo("", 1)));
        assertFalse(index.isValidToken(new TokenInfo(null, 1)));
        assertFalse(index.isValidToken(null));
    }
}
