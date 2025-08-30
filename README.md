# Search Engine - Text Indexing Library

A lightweight, extensible text indexing library built in pure Java 21. Provides fast file indexing and search capabilities with support for concurrent access, filesystem monitoring, and interface-based tokenization algorithms.

## 🚀 Features

- **Fast Indexing**: Efficient in-memory inverted index with concurrent access
- **Real-time Monitoring**: Filesystem watching with automatic index updates
- **Extensible Tokenization**: Interface-based system for custom text processing strategies
- **Performance Monitoring**: Built-in metrics and memory tracking
- **No External Dependencies**: Pure Java implementation
- **Comprehensive Testing**: Unit tests with high coverage

## 🏗️ Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    TextIndexingService                      │
├─────────────────────────────────────────────────────────────┤
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────┐ │
│  │   Indexer   │  │   Searcher  │  │  FileSystemWatcher  │ │
│  │ (ThreadSafe)│  │ (Direct)    │  │  (Event-Driven)     │ │
│  └─────────────┘  └─────────────┘  └─────────────────────┘ │
├─────────────────────────────────────────────────────────────┤
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────┐ │
│  │   Index     │  │  Tokenizer  │  │ Performance Monitor │ │
│  │(In-Memory)  │  │ (Word-based)│  │ (Metrics) ✅        │ │
│  └─────────────┘  └─────────────┘  └─────────────────────┘ │
└─────────────────────────────────────────────────────────────┘
```

## 🚀 Quick Start

### Prerequisites
- Java 21 (JDK)
- Maven 3.6+

### 📁 Supported File Types
The search engine automatically indexes these file types:
- **Text Files**: `.txt`, `.md`
- **Source Code**: `.java`, `.py`, `.js`, `.go`, `.sh`
- **Data Files**: `.xml`, `.json`
- **Custom**: Add your own extensions via code changes

### Build & Run
```bash
# Build the project
mvn clean package

# Run the application
java -jar target/SearchEngine-1.0-SNAPSHOT-jar-with-dependencies.jar ./searchengine.properties
```

**📖 For detailed build instructions, configuration, and usage examples, see [USER_GUIDE.md](USER_GUIDE.md)**

### Basic Usage
```java
import com.demo.searchengine.service.TextIndexingService;
import com.demo.searchengine.service.SimpleTextIndexingService;
import com.demo.searchengine.core.SearchResult;
import com.demo.searchengine.performance.PerformanceMetrics;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

// Create the service
TextIndexingService service = new SimpleTextIndexingService();

// Index a directory
service.indexAllFilesInDirectory(Path.of("documents/"));

// Search for terms
List<SearchResult> results = service.searchAll(Arrays.asList("java", "programming"));

// Get performance metrics
PerformanceMetrics metrics = service.getPerformanceMetrics();
```

## 🔧 Extending the Library

### Custom Tokenizer
```java
import com.demo.searchengine.tokenizer.Tokenizer;
import com.demo.searchengine.tokenizer.model.Token;
import java.util.List;

public class MyCustomTokenizer implements Tokenizer {
    @Override
    public List<Token> tokenize(String content) {
        // Your custom tokenization logic
    }
}

// Use with service
TextIndexingService service = new SimpleTextIndexingService(new MyCustomTokenizer());
```

### Custom Index Implementation
```java
import com.demo.searchengine.core.Index;

public class MyCustomIndex implements Index {
    // Implement Index interface methods
}

// Use with service
service.setIndex(new MyCustomIndex());
```

**📖 For advanced extension patterns and examples, see [USER_GUIDE.md](USER_GUIDE.md)**

## 🧪 Testing

```bash
# Run all tests
mvn test
```

**📖 For detailed testing commands, categories, and reports, see [USER_GUIDE.md](USER_GUIDE.md)**

## 📚 Documentation

- **[User Guide](USER_GUIDE.md)** - Complete usage instructions, configuration, and examples
- **[Tokenization Extension Guide](TOKENIZATION_EXTENSION_GUIDE.md)** - How to extend the library with custom tokenization algorithms
- **[Concurrency & Filesystem Watching](CONCURRENCY_AND_WATCHING.md)** - How the library handles concurrent access and filesystem changes
- **[Testing](TESTING.md)** - Testing strategy, coverage (81%), and test types
- **[Development Log](DEVELOPMENT_LOG.md)** - Current status, future work, and ideas
- **[Requirements](REQUIREMENTS.md)** - Original task requirements

## 🎯 Current Status

**✅ COMPLETE**: Core library with filesystem watching, concurrent access, performance monitoring, and unit testing.

**🔄 NEXT**: Advanced search features and performance optimizations.

See [DEVELOPMENT_LOG.md](DEVELOPMENT_LOG.md) for detailed status.


