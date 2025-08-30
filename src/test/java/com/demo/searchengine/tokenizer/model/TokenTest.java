package com.demo.searchengine.tokenizer.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * Tests for Token interface implementations.
 */
class TokenTest {

    @Test
    void testTokenInfoImplementsToken() {
        final TokenInfo token = new TokenInfo("hello", 5);

        // Test interface methods
        assertEquals("hello", token.getValue());
        assertEquals(5, token.getLineNumber());
        assertEquals(TokenType.WORD.getDefaultRelevance(), token.getRelevance());
        assertEquals(TokenType.WORD, token.getType());
        assertEquals(0, token.getColumn()); // Default implementation
    }

    @Test
    void testTokenTypeDefaultRelevance() {
        assertEquals(1.0, TokenType.KEYWORD.getDefaultRelevance());
        assertEquals(0.9, TokenType.IDENTIFIER.getDefaultRelevance());
        assertEquals(0.8, TokenType.STRING.getDefaultRelevance());
        assertEquals(0.7, TokenType.NUMBER.getDefaultRelevance());
        assertEquals(0.6, TokenType.WORD.getDefaultRelevance());
        assertEquals(0.4, TokenType.COMMENT.getDefaultRelevance());
        assertEquals(0.2, TokenType.PUNCTUATION.getDefaultRelevance());
        assertEquals(0.5, TokenType.UNKNOWN.getDefaultRelevance());
    }
}
