package com.demo.searchengine.tokenizer.model;

/**
 * Represents a token with its line information in the source text. Implements Token interface for backward
 * compatibility.
 */
public record TokenInfo(String token, int lineNumber) implements Token {

    @Override
    public String getValue() {
        return token;
    }

    @Override
    public int getLineNumber() {
        return lineNumber;
    }

    @Override
    public double getRelevance() {
        return TokenType.WORD.getDefaultRelevance();
    }

    @Override
    public TokenType getType() {
        return TokenType.WORD;
    }

    @Override
    public String toString() {
        return "TokenInfo{token='" + token + "', line=" + lineNumber + "}";
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;

        final TokenInfo that = (TokenInfo) obj;
        return lineNumber == that.lineNumber && token.equals(that.token);
    }

}
