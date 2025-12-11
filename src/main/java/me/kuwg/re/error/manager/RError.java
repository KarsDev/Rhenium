package me.kuwg.re.error.manager;

public abstract class RError {
    private final String message;
    private final int line;

    protected RError(final String message, final int line) {
        this.message = message;
        this.line = line;
    }

    final String getMessage() {
        return message;
    }

    final int getLine() {
        return line;
    }

    protected abstract int getCode();

    public @SuppressWarnings("unchecked")
    final <T> T raise() {
        ErrorManager.raise(this);
        return (T) RError.class;
    }
}
