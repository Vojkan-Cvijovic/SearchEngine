package com.demo.searchengine.tokenizer.model;

/**
 * Enumeration of token types for categorization and relevance scoring.
 */
public enum TokenType {

        /**
         * Programming language keywords (e.g., public, class, if, while). Highest relevance for code search.
         */
        KEYWORD(1.0),

        /**
         * Identifiers like variable names, class names, method names. High relevance for code search.
         */
        IDENTIFIER(0.9),

        /**
         * String literals and text content. Medium-high relevance for content search.
         */
        STRING(0.8),

        /**
         * Numeric values and constants. Medium relevance.
         */
        NUMBER(0.7),

        /**
         * Regular words from natural language text. Standard relevance for general search.
         */
        WORD(0.6),

        /**
         * Comments in code or documentation. Lower relevance but still searchable.
         */
        COMMENT(0.4),

        /**
         * Punctuation and special characters. Lowest relevance, mainly for exact matching.
         */
        PUNCTUATION(0.2),

        /**
         * Unknown or undefined token type. Default relevance.
         */
        UNKNOWN(0.5);

    private final double defaultRelevance;

    TokenType(final double defaultRelevance) {
        this.defaultRelevance = defaultRelevance;
    }

    /**
     * Gets the default relevance score for this token type.
     * @return default relevance between 0.0 and 1.0
     */
    public double getDefaultRelevance() {
        return defaultRelevance;
    }
}
