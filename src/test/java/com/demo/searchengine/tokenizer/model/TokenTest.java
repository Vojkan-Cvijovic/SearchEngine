package com.demo.searchengine.tokenizer.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
    void testEnhancedTokenBasic() {
        final EnhancedToken token = new EnhancedToken("public", TokenType.KEYWORD, 10, 5);

        // Test interface methods
        assertEquals("public", token.getValue());
        assertEquals(10, token.getLineNumber());
        assertEquals(TokenType.KEYWORD.getDefaultRelevance(), token.getRelevance());
        assertEquals(TokenType.KEYWORD, token.getType());
        assertEquals(5, token.getColumn());
    }

    @Test
    void testEnhancedTokenCustomRelevance() {
        final EnhancedToken token = new EnhancedToken("important", TokenType.IDENTIFIER, 15, 8, 0.95);

        assertEquals("important", token.getValue());
        assertEquals(15, token.getLineNumber());
        assertEquals(0.95, token.getRelevance());
        assertEquals(TokenType.IDENTIFIER, token.getType());
        assertEquals(8, token.getColumn());
    }

    @Test
    void testEnhancedTokenValidation() {
        // Test null value
        assertThrows(IllegalArgumentException.class, () -> new EnhancedToken(null, TokenType.WORD, 1, 1, 0.5));

        // Test empty value
        assertThrows(IllegalArgumentException.class, () -> new EnhancedToken("", TokenType.WORD, 1, 1, 0.5));

        // Test null type
        assertThrows(IllegalArgumentException.class, () -> new EnhancedToken("test", null, 1, 1, 0.5));

        // Test invalid line number
        assertThrows(IllegalArgumentException.class, () -> new EnhancedToken("test", TokenType.WORD, 0, 1, 0.5));

        // Test invalid column
        assertThrows(IllegalArgumentException.class, () -> new EnhancedToken("test", TokenType.WORD, 1, 0, 0.5));

        // Test invalid relevance
        assertThrows(IllegalArgumentException.class, () -> new EnhancedToken("test", TokenType.WORD, 1, 1, -0.1));

        assertThrows(IllegalArgumentException.class, () -> new EnhancedToken("test", TokenType.WORD, 1, 1, 1.1));
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

    @Test
    void testEnhancedTokenWithRelevance() {
        final EnhancedToken original = new EnhancedToken("test", TokenType.WORD, 1, 1);
        final EnhancedToken updated = original.withRelevance(0.8);

        assertEquals(0.8, updated.getRelevance());
        assertEquals(original.getValue(), updated.getValue());
        assertEquals(original.getType(), updated.getType());
        assertEquals(original.getLineNumber(), updated.getLineNumber());
        assertEquals(original.getColumn(), updated.getColumn());
    }

    @Test
    void testEnhancedTokenWithPosition() {
        final EnhancedToken original = new EnhancedToken("test", TokenType.WORD, 1, 1);
        final EnhancedToken updated = original.withPosition(5, 10);

        assertEquals(5, updated.getLineNumber());
        assertEquals(10, updated.getColumn());
        assertEquals(original.getValue(), updated.getValue());
        assertEquals(original.getType(), updated.getType());
        assertEquals(original.getRelevance(), updated.getRelevance());
    }
}
