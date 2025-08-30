package com.demo.searchengine.tokenizer.impl;

import java.util.List;

import com.demo.searchengine.tokenizer.Tokenizer;
import com.demo.searchengine.tokenizer.model.Token;
import com.demo.searchengine.tokenizer.model.TokenInfo;

/**
 * Simple word-based tokenizer that splits text on whitespace and punctuation. This is a basic implementation suitable
 * for most text files.
 */
public class SimpleWordTokenizer implements Tokenizer {

    private final String name = "SimpleWordTokenizer";
    private final boolean toLowerCase;
    private final int minWordLength;

    /**
     * Creates tokenizer with default settings.
     */
    public SimpleWordTokenizer() {
        this(true, 2);
    }

    /**
     * Creates tokenizer with custom settings.
     * @param toLowerCase whether to convert to lowercase
     * @param minWordLength minimum word length
     */
    public SimpleWordTokenizer(final boolean toLowerCase, final int minWordLength) {
        this.toLowerCase = toLowerCase;
        this.minWordLength = Math.max(1, minWordLength);
    }

    /**
     * Tokenizes text into words with line numbers.
     * @param text the text to tokenize
     * @return list of token information
     * @throws IllegalArgumentException if a text is null
     */
    @Override
    public List<Token> tokenize(final String text) {
        if (text == null) {
            throw new IllegalArgumentException("Text cannot be null");
        }

        if (text.trim()
                .isEmpty()) {
            return List.of();
        }

        // Split text into lines and process each line
        final String[] lines = text.split("\n", -1); // -1 to keep empty lines
        final List<Token> tokens = new java.util.ArrayList<>();

        for (int lineNum = 0; lineNum < lines.length; lineNum++) {
            final String line = lines[lineNum];
            final int actualLineNumber = lineNum + 1; // Convert to 1-based line numbers

            // Split line into words and process each word
            final String[] words = line.split("\\s+");

            for (final String word : words) {
                final String cleanedToken = cleanToken(word);
                if (isValidToken(cleanedToken)) {
                    tokens.add(new TokenInfo(cleanedToken, actualLineNumber));
                }
            }
        }

        return tokens;
    }

    /**
     * Returns tokenizer name.
     * @return tokenizer name
     */
    @Override
    public String getName() {
        return name;
    }

    /**
     * Clean a token by removing punctuation and applying case conversion.
     */
    String cleanToken(final String token) {
        if (token == null || token.isEmpty()) {
            return "";
        }

        // Remove leading/trailing punctuation
        String cleaned = token.replaceAll("^[\\p{Punct}\\s]+|[\\p{Punct}\\s]+$", "");

        // Apply case conversion if enabled
        if (toLowerCase) {
            cleaned = cleaned.toLowerCase();
        }

        return cleaned;
    }

    /**
     * Check if a token is valid (meets minimum length requirement).
     */
    boolean isValidToken(final String token) {
        return token != null && token.length() >= minWordLength;
    }

    @Override
    public String toString() {
        return String.format("SimpleWordTokenizer{name='%s', minLength=%d, toLowerCase=%s}", name, minWordLength,
            toLowerCase);
    }
}
