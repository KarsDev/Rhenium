package me.kuwg.re.token;

import me.kuwg.re.util.TokenUtil;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class Tokenizer {
    private static final String[] KEYWORDS = TokenUtil.Keywords.loadKeywords();
    private static final String[] OPERATORS = TokenUtil.Operators.loadOperators();
    private static final Character[] DIVIDERS = TokenUtil.Dividers.loadDividers();

    private final List<Token> tokens;
    private final BufferedReader reader;
    private int currentChar;
    private int line;

    public static List<Token> tokenize(InputStream inputStream) throws IOException {
        final Tokenizer tokenizer = new Tokenizer(inputStream);
        tokenizer.tokenize0();
        return tokenizer.get();
    }

    private Tokenizer(InputStream inputStream) throws IOException {
        this.tokens = new ArrayList<>();
        this.reader = new BufferedReader(new InputStreamReader(inputStream));
        this.currentChar = reader.read();
        this.line = 1;
    }

    private void tokenize0() throws IOException {
        while (currentChar != -1) {
            char ch = (char) currentChar;

            if (Character.isWhitespace(ch)) {
                if (ch == '\n') {
                    tokens.add(new Token(null, TokenType.NEWLINE, line));
                    line++;
                }
                consume();
                continue;
            }

            if (Character.isLetter(ch) || ch == '_') {
                tokens.add(readWord());
            } else if (Character.isDigit(ch)) {
                tokens.add(readNumber());
            } else if (isDivider(ch)) {
                tokens.add(new Token(String.valueOf(ch), TokenType.DIVIDER, line));
                consume();
            } else {
                tokens.add(readOperatorOrUnknown());
            }
        }
    }

    private Token readWord() throws IOException {
        StringBuilder builder = new StringBuilder();
        while (currentChar != -1 && (Character.isLetterOrDigit((char) currentChar) || (char) currentChar == '_')) {
            builder.append((char) currentChar);
            consume();
        }
        String word = builder.toString();
        if (isKeyword(word)) {
            return new Token(word, TokenType.KEYWORD, line);
        } else {
            return new Token(word, TokenType.IDENTIFIER, line);
        }
    }

    private Token readNumber() throws IOException {
        StringBuilder builder = new StringBuilder();
        while (currentChar != -1 && Character.isDigit((char) currentChar)) {
            builder.append((char) currentChar);
            consume();
        }
        return new Token(builder.toString(), TokenType.NUMBER, line);
    }

    private Token readOperatorOrUnknown() throws IOException {
        StringBuilder builder = new StringBuilder();
        builder.append((char) currentChar);
        consume();

        // try matching multi-character operators
        if (currentChar != -1) {
            builder.append((char) currentChar);
            String op = builder.toString();
            if (isOperator(op)) {
                consume();
                return new Token(op, TokenType.OPERATOR, line);
            }
            builder.setLength(1); // back to 1 char
        }

        String single = builder.toString();
        if (isOperator(single)) {
            return new Token(single, TokenType.OPERATOR, line);
        }

        throw new RuntimeException("Unknown value: " + single);
    }

    private boolean isKeyword(String word) {
        for (String keyword : KEYWORDS) {
            if (keyword.equals(word)) {
                return true;
            }
        }
        return false;
    }

    private boolean isOperator(String op) {
        for (String operator : OPERATORS) {
            if (operator.equals(op)) {
                return true;
            }
        }
        return false;
    }

    private boolean isDivider(char ch) {
        for (Character divider : DIVIDERS) {
            if (divider == ch) {
                return true;
            }
        }
        return false;
    }

    private void consume() throws IOException {
        currentChar = reader.read();
    }

    private List<Token> get() {
        return tokens;
    }
}