# Tokenization Extension Guide - Search Engine Library

A focused guide for extending the Search Engine library with custom tokenization algorithms and implementations.

## 🎯 Overview

The Search Engine library is designed with extensibility in mind, particularly for tokenization algorithms. You can extend it in three main ways:

1. **Custom Tokenizer** - Implement new text processing algorithms
2. **Custom Token** - Create specialized token types with custom behavior  
3. **Custom Service** - Extend or modify the indexing service behavior

## 🔧 1. Custom Tokenizer Implementation

### Interface to Implement

```java
public interface Tokenizer {
    /**
     * Tokenize the given text into a list of terms with position information.
     * @param text the text to tokenize
     * @return list of tokens with their line numbers and positions
     * @throws IllegalArgumentException if a text is null
     */
    List<Token> tokenize(String text);

    /**
     * Get the name of this tokenizer.
     * @return tokenizer name
     */
    String getName();
}
```

### Basic Implementation

```java
public class MyCustomTokenizer implements Tokenizer {
    @Override
    public List<Token> tokenize(String text) {
        if (text == null) {
            throw new IllegalArgumentException("Text cannot be null");
        }
        
        List<Token> tokens = new ArrayList<>();
        // Your tokenization logic here
        // Split text, process lines, create tokens
        
        return tokens;
    }

    @Override
    public String getName() {
        return "MyCustomTokenizer";
    }
}
```

### Usage

```java
// Create service with custom tokenizer
Tokenizer customTokenizer = new MyCustomTokenizer();
TextIndexingService service = new SimpleTextIndexingService(customTokenizer);
```

## 🏷️ 2. Custom Token Implementation

### Interface to Implement

```java
public interface Token {
    /**
     * Gets the actual text value of the token.
     * @return the token text
     */
    String getValue();

    /**
     * Gets the line number where this token appears.
     * @return the line number (1-based)
     */
    int getLineNumber();

    /**
     * Gets the relevance score for this token.
     * @return relevance score between 0.0 and 1.0
     */
    double getRelevance();

    /**
     * Gets the column position of this token (optional).
     * @return the column position, or 0 if not available
     */
    default int getColumn() {
        return 0;
    }

    /**
     * Gets the type of this token (optional).
     * @return the token type, or UNKNOWN if not specified
     */
    default TokenType getType() {
        return TokenType.UNKNOWN;
    }
}
```

### Basic Implementation

```java
public class MyCustomToken implements Token {
    private final String value;
    private final int lineNumber;
    private final double relevance;

    public MyCustomToken(String value, int lineNumber, double relevance) {
        this.value = value;
        this.lineNumber = lineNumber;
        this.relevance = relevance;
    }

    @Override
    public String getValue() { return value; }
    
    @Override
    public int getLineNumber() { return lineNumber; }
    
    @Override
    public double getRelevance() { return relevance; }
}
```

## 🚀 3. Custom Service Implementation

### Interface to Extend

```java
public interface TextIndexingService {
    boolean indexFile(Path filePath);
    int indexAllFilesInDirectory(Path directoryPath);
    boolean removeFile(Path filePath);
    List<SearchResult> searchAll(List<String> terms);
    PerformanceMetrics getPerformanceMetrics();
}
```

### Basic Implementation

```java
public class MyCustomService implements TextIndexingService {
    private final Tokenizer tokenizer;
    private final Index index;

    public MyCustomService(Tokenizer tokenizer) {
        this.tokenizer = tokenizer;
        this.index = new ThreadSafeIndex();
    }

    @Override
    public boolean indexFile(Path filePath) {
        // Your custom indexing logic here
        return true;
    }

    @Override
    public int indexAllFilesInDirectory(Path directoryPath) {
        // Your custom directory indexing logic here
        return 0;
    }

    @Override
    public boolean removeFile(Path filePath) {
        // Your custom file removal logic here
        return true;
    }

    @Override
    public List<SearchResult> searchAll(List<String> terms) {
        // Your custom search logic here
        return List.of();
    }

    @Override
    public PerformanceMetrics getPerformanceMetrics() {
        // Your custom metrics logic here
        return new PerformanceMetrics();
    }
}
```

## 📋 Best Practices

### Performance
- Compile regex patterns once in constructor
- Reuse objects where possible
- Consider streaming for large files

### Error Handling
- Return empty lists instead of throwing exceptions
- Log errors for debugging
- Validate inputs early

### Testing
- Test individual components
- Test integration with existing system
- Test edge cases and error conditions

## 🎉 Summary

To extend the Search Engine library:

1. **Implement `Tokenizer`** - Define how text is processed
2. **Implement `Token`** - Define token structure and metadata  
3. **Implement `TextIndexingService`** - Define indexing and search behavior

The library's interface-based design ensures your custom implementations work seamlessly with the existing system.

For examples, see the built-in implementations in `src/main/java/com/demo/searchengine/`.

## 📚 Related Documentation

- **[Concurrency & Filesystem Watching](CONCURRENCY_AND_WATCHING.md)** - How the library handles concurrent access and filesystem changes
- **[User Guide](USER_GUIDE.md)** - Complete usage instructions and examples
- **[README](README.md)** - Project overview and quick start
