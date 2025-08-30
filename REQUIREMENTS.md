# Original Task Requirements

## Overview
Create a Java or Kotlin library that provides a service for indexing text files.

## Original Task Quote
> "The library interface should allow for specifying the indexed files and directories and querying files containing a given word. The library should support concurrent access and react to changes in the (watched part of) filesystem. The library should be extensible by the tokenization algorithm (simple splitting by words/support lexers/etc)"

## Core Requirements Breakdown

### 1. **Library Interface for Indexing Service**
- **Multi-user Support**: Library must be usable by many users simultaneously
- **Configurable**: Users can specify which files and directories to index
- **Query Interface**: Users can search for files containing specific words

### 2. **Concurrent Access Support**
- **Thread Safety**: Multiple users can access the service at the same time
- **Shared Index**: All users work with the same underlying index
- **No Conflicts**: Concurrent operations don't interfere with each other

### 3. **Filesystem Change Detection**
- **Watch Directories**: Monitor specific parts of the filesystem
- **React to Changes**: Automatically update index when files change
- **Real-time Updates**: Index stays current without manual intervention

### 4. **Extensible Tokenization System**
- **Interface-Based Extension**: Users can implement the `Tokenizer` interface
- **Algorithm Selection**: Users choose which tokenization method to use
- **Examples Provided**: Simple word splitting, lexer support, and more
- **Easy Extension**: Adding new algorithms by implementing the interface
