package com.demo.searchengine.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.demo.searchengine.core.FileLocation;
import com.demo.searchengine.core.FileMetadata;
import com.demo.searchengine.core.Index;
import com.demo.searchengine.core.SearchResult;
import com.demo.searchengine.performance.PerformanceMetrics;
import com.demo.searchengine.performance.PerformanceMonitor;
import com.demo.searchengine.tokenizer.Tokenizer;
import com.demo.searchengine.tokenizer.model.Token;
import com.demo.searchengine.tokenizer.model.TokenInfo;
import com.demo.searchengine.util.FileFilter;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SimpleTextIndexingServiceTest {

    @Mock
    private Tokenizer mockTokenizer;

    @Mock
    private FileFilter mockFileFilter;

    @Mock
    private Index mockIndex;

    @Mock
    private PerformanceMonitor mockPerformanceMonitor;

    @TempDir
    private Path tempDir;

    private SimpleTextIndexingService service;
    private Path testFile;

    @BeforeEach
    void setUp() throws IOException {
        service = new SimpleTextIndexingService(mockTokenizer);

        service.setFileFilter(mockFileFilter);
        service.setIndex(mockIndex);
        service.setPerformanceMonitor(mockPerformanceMonitor);

        when(mockFileFilter.shouldIndex(any(Path.class))).thenReturn(true);
        when(mockPerformanceMonitor.getMetrics()).thenReturn(new PerformanceMetrics());

        doNothing().when(mockIndex)
                .addTermsWithLineInfo(any(), any());
        doNothing().when(mockIndex)
                .updateTermsForFile(any(), any());
        doNothing().when(mockIndex)
                .addFileMetadata(any());
        doNothing().when(mockIndex)
                .removeFile(any());
        doNothing().when(mockIndex)
                .clear();
        when(mockIndex.getFileMetadata(any())).thenReturn(null);
        when(mockIndex.getFileCount()).thenReturn(0);
        when(mockIndex.getTotalTerms()).thenReturn(0);
        when(mockIndex.getUniqueTerms()).thenReturn(0);
        when(mockIndex.isEmpty()).thenReturn(true);
        when(mockIndex.findAllTerms(any())).thenReturn(Set.of());

        testFile = tempDir.resolve("test.txt");
        Files.writeString(testFile, "hello world test content");

        final Path testDirectory = tempDir.resolve("test");
        Files.createDirectory(testDirectory);
        Files.writeString(testDirectory.resolve("file1.txt"), "content one");
        Files.writeString(testDirectory.resolve("file2.md"), "content two");
        Files.writeString(testDirectory.resolve("file3.java"), "public class Test");
    }

    @Test
    void constructor_WithNullTokenizer_ThrowsException() {
        assertThrows(NullPointerException.class, () -> new SimpleTextIndexingService(null));
    }

    @Test
    void indexFile_WithNullFilePath_ThrowsException() {
        assertThrows(NullPointerException.class, () -> service.indexFile(null));
    }

    @Test
    void indexFiles_WithNullList_ThrowsException() {
        assertThrows(NullPointerException.class, () -> service.indexFiles(null));
    }

    @Test
    void indexAllFilesInDirectory_WithNullPath_ThrowsException() {
        assertThrows(NullPointerException.class, () -> service.indexAllFilesInDirectory(null));
    }

    @Test
    void searchAll_WithSingleTerm_ReturnsResults() {

        final FileLocation mockLocation = new FileLocation(testFile, 1);
        when(mockIndex.findAllTerms(List.of("hello"))).thenReturn(Set.of(mockLocation));

        final FileMetadata mockMetadata = new FileMetadata(testFile, 100L, java.time.Instant.now(), 1, 1);
        when(mockIndex.getFileMetadata(testFile)).thenReturn(mockMetadata);

        final List<SearchResult> results = service.searchAll(List.of("hello"));

        assertEquals(1, results.size());
    }

    @Test
    void searchAll_WithValidTerms_ReturnsResults() {

        final FileLocation mockLocation = new FileLocation(testFile, 1);
        when(mockIndex.findAllTerms(Arrays.asList("hello", "world"))).thenReturn(Set.of(mockLocation));

        final FileMetadata mockMetadata = new FileMetadata(testFile, 100L, java.time.Instant.now(), 2, 2);
        when(mockIndex.getFileMetadata(testFile)).thenReturn(mockMetadata);

        final List<SearchResult> results = service.searchAll(Arrays.asList("hello", "world"));

        assertEquals(1, results.size());
    }

    @Test
    void searchAll_WithTermsOnSameLine_ReturnsResults() throws IOException {
        final Path sameLineFile = tempDir.resolve("same line.txt");
        Files.writeString(sameLineFile, "hello world");

        final FileLocation mockLocation = new FileLocation(sameLineFile, 1);
        when(mockIndex.findAllTerms(Arrays.asList("hello", "world"))).thenReturn(Set.of(mockLocation));

        final FileMetadata mockMetadata = new FileMetadata(sameLineFile, 100L, java.time.Instant.now(), 2, 2);
        when(mockIndex.getFileMetadata(sameLineFile)).thenReturn(mockMetadata);

        final List<SearchResult> results = service.searchAll(Arrays.asList("hello", "world"));

        assertEquals(1, results.size());
    }

    @Test
    void searchAll_WithNullTerms_ReturnsEmptyList() {
        final List<SearchResult> results = service.searchAll(null);

        assertTrue(results.isEmpty());
    }

    @Test
    void searchAll_WithEmptyTerms_ReturnsEmptyList() {
        final List<SearchResult> results = service.searchAll(List.of());

        assertTrue(results.isEmpty());
    }

    @Test
    void getFileMetadata_WithIndexedFile_ReturnsMetadata() {

        service.indexedFiles.add(testFile);

        final FileMetadata mockMetadata = new FileMetadata(testFile, 100L, java.time.Instant.now(), 1, 1);
        when(mockIndex.getFileMetadata(testFile)).thenReturn(mockMetadata);

        final FileMetadata metadata = service.getFileMetadata(testFile);

        assertNotNull(metadata);
        assertEquals(testFile, metadata.getFilePath());
    }

    @Test
    void getFileMetadata_WithNonIndexedFile_ReturnsNull() {
        final FileMetadata metadata = service.getFileMetadata(testFile);

        assertNull(metadata);
    }

    @Test
    void getFileMetadata_WithNullPath_ReturnsNull() {
        final FileMetadata metadata = service.getFileMetadata(null);

        assertNull(metadata);
    }

    @Test
    void validateFileForIndexing_WithValidFile_ReturnsTrue() {
        final Path validFile = tempDir.resolve("valid.txt");
        try {
            Files.writeString(validFile, "content");
        } catch (final IOException e) {
            fail("Failed to create test file");
        }

        when(mockFileFilter.shouldIndex(validFile)).thenReturn(true);

        final boolean result = service.validateFileForIndexing(validFile);

        assertTrue(result);
        verify(mockFileFilter).shouldIndex(validFile);
    }

    @Test
    void validateFileForIndexing_WithNonExistentFile_ReturnsFalse() {
        final Path nonExistentFile = tempDir.resolve("nonexistent.txt");

        final boolean result = service.validateFileForIndexing(nonExistentFile);

        assertFalse(result);

        verify(mockFileFilter, never()).shouldIndex(any());
    }

    @Test
    void validateFileForIndexing_WithDirectory_ReturnsFalse() {
        final Path directory = tempDir.resolve("directory");
        try {
            Files.createDirectory(directory);
        } catch (final IOException e) {
            fail("Failed to create test directory");
        }

        final boolean result = service.validateFileForIndexing(directory);

        assertFalse(result);

        verify(mockFileFilter, never()).shouldIndex(any());
    }

    @Test
    void validateFileForIndexing_WithUnsupportedFile_ReturnsFalse() {
        final Path unsupportedFile = tempDir.resolve("unsupported.pdf");
        try {
            Files.writeString(unsupportedFile, "content");
        } catch (final IOException e) {
            fail("Failed to create test file");
        }

        when(mockFileFilter.shouldIndex(unsupportedFile)).thenReturn(false);

        final boolean result = service.validateFileForIndexing(unsupportedFile);

        assertFalse(result);
        verify(mockFileFilter).shouldIndex(unsupportedFile);
    }

    @Test
    void readAndValidateFileContent_WithValidContent_ReturnsContent() {
        final Path validFile = tempDir.resolve("valid.txt");
        final String expectedContent = "hello world content";
        try {
            Files.writeString(validFile, expectedContent);
        } catch (final IOException e) {
            fail("Failed to create test file");
        }

        final String result = service.readAndValidateFileContent(validFile);

        assertEquals(expectedContent, result);
    }

    @Test
    void readAndValidateFileContent_WithEmptyFile_ReturnsNull() {
        final Path emptyFile = tempDir.resolve("empty.txt");
        try {
            Files.writeString(emptyFile, "");
        } catch (final IOException e) {
            fail("Failed to create test file");
        }

        final String result = service.readAndValidateFileContent(emptyFile);

        assertNull(result);
    }

    @Test
    void readAndValidateFileContent_WithWhitespaceOnlyFile_ReturnsNull() {
        final Path whitespaceFile = tempDir.resolve("whitespace.txt");
        try {
            Files.writeString(whitespaceFile, "   \n\t  ");
        } catch (final IOException e) {
            fail("Failed to create test file");
        }

        final String result = service.readAndValidateFileContent(whitespaceFile);

        assertNull(result);
    }

    @Test
    void processTokensAndUpdateIndex_WithNewFile_AddsToIndex() {
        final Path newFile = tempDir.resolve("new.txt");
        final List<Token> tokenInfos = Arrays.asList(new TokenInfo("hello", 1), new TokenInfo("world", 2));

        doNothing().when(mockIndex)
                .addTermsWithLineInfo(any(), any());

        service.processTokensAndUpdateIndex(newFile, tokenInfos);

        verify(mockIndex).addTermsWithLineInfo(tokenInfos, newFile);
        verify(mockIndex, never()).updateTermsForFile(any(), any());
    }

    @Test
    void processTokensAndUpdateIndex_WithExistingFile_UpdatesIndex() {
        final Path existingFile = tempDir.resolve("existing.txt");
        final List<Token> tokenInfos = Arrays.asList(new TokenInfo("updated", 1), new TokenInfo("content", 2));

        service.indexedFiles.add(existingFile);

        doNothing().when(mockIndex)
                .updateTermsForFile(any(), any());

        service.processTokensAndUpdateIndex(existingFile, tokenInfos);

        verify(mockIndex).updateTermsForFile(tokenInfos, existingFile);
        verify(mockIndex, never()).addTermsWithLineInfo(any(), any());
    }

    @Test
    void createAndAddFileMetadata_WithValidTokens_CreatesMetadata() {
        final Path testFile = tempDir.resolve("metadata.txt");
        final List<Token> tokenInfos =
            Arrays.asList(new TokenInfo("hello", 1), new TokenInfo("world", 2), new TokenInfo("hello", 3)

            );

        try {
            Files.writeString(testFile, "hello world hello");
        } catch (final IOException e) {
            fail("Failed to create test file");
        }

        doNothing().when(mockIndex)
                .addFileMetadata(any());

        service.createAndAddFileMetadata(testFile, tokenInfos);

        verify(mockIndex).addFileMetadata(any(FileMetadata.class));
    }

    @Test
    void processFileContent_WithValidContent_ReturnsTrue() {
        final Path testFile = tempDir.resolve("process.txt");
        final String content = "hello world content";
        try {
            Files.writeString(testFile, content);
        } catch (final IOException e) {
            fail("Failed to create test file");
        }

        final List<Token> tokenInfos =
            Arrays.asList(new TokenInfo("hello", 1), new TokenInfo("world", 2), new TokenInfo("content", 3));

        when(mockTokenizer.tokenize(content)).thenReturn(tokenInfos);
        doNothing().when(mockIndex)
                .addTermsWithLineInfo(any(), any());
        doNothing().when(mockIndex)
                .addFileMetadata(any());
        when(mockIndex.getTotalTerms()).thenReturn(3);

        final boolean result = service.processFileContent(testFile);

        assertTrue(result);

        verify(mockTokenizer).tokenize(content);
        verify(mockIndex).addTermsWithLineInfo(tokenInfos, testFile);
        verify(mockIndex).addFileMetadata(any(FileMetadata.class));
    }

    @Test
    void processFileContent_WithEmptyContent_ReturnsFalse() {
        final Path testFile = tempDir.resolve("empty.txt");
        try {
            Files.writeString(testFile, "");
        } catch (final IOException e) {
            fail("Failed to create test file");
        }

        final boolean result = service.processFileContent(testFile);

        assertFalse(result);

        verify(mockIndex, never()).addTermsWithLineInfo(any(), any());
        verify(mockIndex, never()).addFileMetadata(any());
    }

    @Test
    void processFileContent_WithNoValidTokens_ReturnsFalse() {
        final Path testFile = tempDir.resolve("tokens.txt");
        final String content = "valid content";
        try {
            Files.writeString(testFile, content);
        } catch (final IOException e) {
            fail("Failed to create test file");
        }

        when(mockTokenizer.tokenize(content)).thenReturn(List.of());

        final boolean result = service.processFileContent(testFile);

        assertFalse(result);

        verify(mockIndex, never()).addTermsWithLineInfo(any(), any());
        verify(mockIndex, never()).addFileMetadata(any());
    }

    @Test
    void getPerformanceMetrics_ReturnsMetrics() {

        final PerformanceMetrics mockMetrics = mock(PerformanceMetrics.class);
        when(mockPerformanceMonitor.getMetrics()).thenReturn(mockMetrics);

        final PerformanceMetrics metrics = service.getPerformanceMetrics();

        assertNotNull(metrics);

        verify(mockPerformanceMonitor).getMetrics();
    }

    @Test
    void isSupportedFile_WithSupportedExtensions_ReturnsTrue() {
        final Path testPath = Path.of("test.txt");
        when(mockFileFilter.shouldIndex(testPath)).thenReturn(true);

        final boolean result = service.isSupportedFile(testPath);

        assertTrue(result);
        verify(mockFileFilter).shouldIndex(testPath);
    }

    @Test
    void isSupportedFile_WithUnsupportedExtensions_ReturnsFalse() {
        final Path testPath = Path.of("image.png");
        when(mockFileFilter.shouldIndex(testPath)).thenReturn(false);

        final boolean result = service.isSupportedFile(testPath);

        assertFalse(result);
        verify(mockFileFilter).shouldIndex(testPath);
    }

    @Test
    void isSupportedFile_WithCaseInsensitiveExtensions_ReturnsTrue() {
        final Path testPath = Path.of("test.TXT");
        when(mockFileFilter.shouldIndex(testPath)).thenReturn(true);

        final boolean result = service.isSupportedFile(testPath);

        assertTrue(result);
        verify(mockFileFilter).shouldIndex(testPath);
    }

    @Test
    void convertToSearchResults_WithEmptyLocations_ReturnsEmptyList() {
        final List<SearchResult> results = service.convertToSearchResults(Set.of(), "test");
        assertTrue(results.isEmpty());
    }

    @Test
    void convertToSearchResults_WithValidLocations_ReturnsResults() {

        final Path testFile = tempDir.resolve("test.txt");
        final FileLocation location1 = new FileLocation(testFile, 1);
        final FileLocation location2 = new FileLocation(testFile, 2);

        final Set<FileLocation> locations = Set.of(location1, location2);
        final List<SearchResult> results = service.convertToSearchResults(locations, "test");

        assertEquals(2, results.size());

        assertEquals(2, results.get(0)
                .getLineNumber());
        assertEquals(1, results.get(1)
                .getLineNumber());
    }
}
