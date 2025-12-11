package me.kuwg.re.token;

public record Token(TokenType type, String value, int line) {
    public boolean matches(TokenType type) {
        return this.type == type;
    }

    public boolean matches(TokenType type, String value) {
        return this.type == type && this.value.equals(value);
    }
}
