package com.demo.searchengine.tokenizer.model;

/**
 * Interface for tokens in the search engine. Provides a common contract for different token implementations.
 */
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
