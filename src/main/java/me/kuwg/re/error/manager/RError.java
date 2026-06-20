package me.kuwg.re.error.manager;

public abstract class RError {
    private final String message;
    private final String fileName;
    private final int line;

    protected RError(final String message, final String fileName, final int line) {
        this.message = message;
        this.fileName = fileName;
        this.line = line;
    }

    final String getMessage() {
        return message;
    }

    public String getFileName() {
        return fileName;
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
