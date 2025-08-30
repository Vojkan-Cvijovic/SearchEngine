# Search Engine - Text Indexing Library

A lightweight, extensible text indexing library built in pure Java 21. Provides fast file indexing and search capabilities with support for concurrent access, filesystem monitoring, and pluggable tokenization algorithms.

## 🚀 Features

- **Fast Indexing**: Efficient in-memory inverted index with concurrent access
- **Real-time Monitoring**: Filesystem watching with automatic index updates
- **Extensible Tokenization**: Plugin system for custom text processing strategies
- **Performance Monitoring**: Built-in metrics, health checks, and memory tracking
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
- **Custom**: Add your own extensions via configuration

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
// Create the service
TextIndexingService service = new SimpleTextIndexingService();

// Index a directory
service.indexDirectory(Path.of("documents/"));

// Search for terms
List<SearchResult> results = service.search("java programming");

// Get performance metrics
PerformanceMetrics metrics = service.getPerformanceMetrics();
```

## 🔧 Extending the Library

### Custom Tokenizer
```java
public class MyCustomTokenizer implements Tokenizer {
    @Override
    public List<TokenInfo> tokenize(String content) {
        // Your custom tokenization logic
    }
}

// Use with service
TextIndexingService service = new SimpleTextIndexingService(new MyCustomTokenizer());
```

### Custom Index Implementation
```java
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
- **[Development Log](DEVELOPMENT_LOG.md)** - Current status, future work, and ideas
- **[Requirements](REQUIREMENTS.md)** - Original task requirements

## 🎯 Current Status

**✅ COMPLETE**: Core library with filesystem watching, concurrent access, performance monitoring, and unit testing.

**🔄 NEXT**: Advanced search features and performance optimizations.

See [PROJECT_STATUS.md](PROJECT_STATUS.md) for detailed status.


