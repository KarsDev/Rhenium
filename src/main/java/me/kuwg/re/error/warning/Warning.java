package me.kuwg.re.error.warning;

public class Warning {
    private final String message;
    private final int line;

    public Warning(final String message, final int line) {
        this.message = message;
        this.line = line;
    }

    public void print() {
        System.err.println("[WARNING]: " + message + " on line " + line);
    }
}
