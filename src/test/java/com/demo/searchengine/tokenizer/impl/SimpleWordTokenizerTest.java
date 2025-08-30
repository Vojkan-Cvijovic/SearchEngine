package com.demo.searchengine.tokenizer.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.demo.searchengine.tokenizer.model.Token;

class SimpleWordTokenizerTest {

    private SimpleWordTokenizer tokenizer;

    @BeforeEach
    void setUp() {
        tokenizer = new SimpleWordTokenizer();
    }

    @Test
    void tokenize_WithValidText_ReturnsTokens() {
        final String text = "hello world test";
        final List<Token> tokens = tokenizer.tokenize(text);

        assertEquals(3, tokens.size());
        assertEquals("hello", tokens.get(0)
                .getValue());
        assertEquals("world", tokens.get(1)
                .getValue());
        assertEquals("test", tokens.get(2)
                .getValue());
    }

    @Test
    void tokenize_WithNullText_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> tokenizer.tokenize(null));
    }

    @Test
    void tokenize_WithEmptyText_ReturnsEmptyList() {
        final String text = "";
        final List<Token> tokens = tokenizer.tokenize(text);
        assertTrue(tokens.isEmpty());
    }

    @Test
    void tokenize_WithWhitespaceOnly_ReturnsEmptyList() {
        final String text = "   \t\n  ";
        final List<Token> tokens = tokenizer.tokenize(text);
        assertTrue(tokens.isEmpty());
    }

    @Test
    void tokenize_WithMultipleLines_ReturnsTokensWithCorrectLineNumbers() {
        final String text = "first line\nsecond line\nthird line";
        final List<Token> tokens = tokenizer.tokenize(text);

        assertEquals(6, tokens.size());
        assertEquals(1, tokens.get(0)
                .getLineNumber());
        assertEquals(1, tokens.get(1)
                .getLineNumber());
        assertEquals(2, tokens.get(2)
                .getLineNumber());
        assertEquals(2, tokens.get(3)
                .getLineNumber());
        assertEquals(3, tokens.get(4)
                .getLineNumber());
        assertEquals(3, tokens.get(5)
                .getLineNumber());
    }

    @Test
    void tokenize_WithPunctuation_RemovesPunctuation() {
        final String text = "hello, world! test.";
        final List<Token> tokens = tokenizer.tokenize(text);

        assertEquals(3, tokens.size());
        assertEquals("hello", tokens.get(0)
                .getValue());
        assertEquals("world", tokens.get(1)
                .getValue());
        assertEquals("test", tokens.get(2)
                .getValue());
    }

    @Test
    void tokenize_WithMixedCase_ConvertsToLowerCase() {
        final String text = "Hello WORLD Test";
        final List<Token> tokens = tokenizer.tokenize(text);

        assertEquals(3, tokens.size());
        assertEquals("hello", tokens.get(0)
                .getValue());
        assertEquals("world", tokens.get(1)
                .getValue());
        assertEquals("test", tokens.get(2)
                .getValue());
    }

    @Test
    void tokenize_WithShortWords_FiltersOutShortWords() {
        final String text = "a b c hello world test";
        final List<Token> tokens = tokenizer.tokenize(text);

        assertEquals(3, tokens.size());
        assertEquals("hello", tokens.get(0)
                .getValue());
        assertEquals("world", tokens.get(1)
                .getValue());
        assertEquals("test", tokens.get(2)
                .getValue());
    }

    @Test
    void tokenize_WithCustomMinLength_FiltersCorrectly() {
        final SimpleWordTokenizer customTokenizer = new SimpleWordTokenizer(true, 4);
        final String text = "short long extreme";
        final List<Token> tokens = customTokenizer.tokenize(text);

        assertEquals(3, tokens.size());
        assertEquals("short", tokens.get(0)
                .getValue());
        assertEquals("long", tokens.get(1)
                .getValue());
        assertEquals("extreme", tokens.get(2)
                .getValue());
    }

    @Test
    void tokenize_WithCustomCaseSensitive_RespectsSetting() {
        final SimpleWordTokenizer caseSensitiveTokenizer = new SimpleWordTokenizer(false, 2);
        final String text = "Hello WORLD Test";
        final List<Token> tokens = caseSensitiveTokenizer.tokenize(text);

        assertEquals(3, tokens.size());
        assertEquals("Hello", tokens.get(0)
                .getValue());
        assertEquals("WORLD", tokens.get(1)
                .getValue());
        assertEquals("Test", tokens.get(2)
                .getValue());
    }

    @Test
    void tokenize_WithEmptyLines_PreservesLineNumbers() {
        final String text = "first\n\nsecond\n\n\nthird";
        final List<Token> tokens = tokenizer.tokenize(text);

        assertEquals(3, tokens.size());
        assertEquals(1, tokens.get(0)
                .getLineNumber());
        assertEquals(3, tokens.get(1)
                .getLineNumber());
        assertEquals(6, tokens.get(2)
                .getLineNumber());
    }

    @Test
    void tokenize_WithSpecialCharacters_HandlesCorrectly() {
        final String text = "hello@world.com test-file_name";
        final List<Token> tokens = tokenizer.tokenize(text);

        assertEquals(2, tokens.size());
        assertEquals("hello@world.com", tokens.get(0)
                .getValue());
        assertEquals("test-file_name", tokens.get(1)
                .getValue());
    }

    @Test
    void getName_WithNullName_UsesDefaultName() {
        final SimpleWordTokenizer nullNameTokenizer = new SimpleWordTokenizer(true, 2);
        assertEquals("SimpleWordTokenizer", nullNameTokenizer.getName());
    }

    @Test
    void toString_ReturnsFormattedString() {
        final String result = tokenizer.toString();
        assertTrue(result.contains("SimpleWordTokenizer"));
        assertTrue(result.contains("minLength=2"));
        assertTrue(result.contains("toLowerCase=true"));
    }

    @Test
    void cleanToken_WithVariousInputs_CleansCorrectly() {
        assertEquals("hello", tokenizer.cleanToken("  Hello  "));
        assertEquals("world", tokenizer.cleanToken("WORLD"));
        assertEquals("test", tokenizer.cleanToken("Test"));
        assertEquals("", tokenizer.cleanToken("   "));
        assertEquals("", tokenizer.cleanToken(null));
        assertEquals("", tokenizer.cleanToken(""));
    }

    @Test
    void isValidToken_WithVariousTokens_ValidatesCorrectly() {
        assertTrue(tokenizer.isValidToken("hello"));
        assertTrue(tokenizer.isValidToken("world"));
        assertFalse(tokenizer.isValidToken("a"));
        assertFalse(tokenizer.isValidToken(""));
        assertFalse(tokenizer.isValidToken(null));
    }
}
