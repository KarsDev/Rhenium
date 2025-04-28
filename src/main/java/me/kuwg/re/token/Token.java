package me.kuwg.re.token;


/**
 * Token class, parsed by the Tokenizer.
 *
 * @param value The string representation of the value
 * @param type The type of the token
 * @param line The line where the token is found
 *
 * @author Kuwg
 *
 * @see me.kuwg.re.token.TokenType
 * @see me.kuwg.re.token.Tokenizer
 */
public record Token(String value, TokenType type, int line) {
}
