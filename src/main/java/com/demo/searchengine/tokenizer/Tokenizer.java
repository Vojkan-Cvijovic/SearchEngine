package com.demo.searchengine.tokenizer;

import java.util.List;

import com.demo.searchengine.tokenizer.model.Token;

/**
 * Interface for text tokenization strategies. Different implementations can provide various tokenization approaches
 * (word-based, lexer-based, regex-based, etc.).
 */
public interface Tokenizer {

    /**
     * Tokenize the given text into a list of terms with position information.
     * @param text the text to tokenize
     * @return list of tokens with their line numbers and positions
     * @throws IllegalArgumentException if a text is null
     */
    List<Token> tokenize(String text);

    /**
     * Get the name of this tokenizer.
     * @return tokenizer name
     */
    String getName();

}
