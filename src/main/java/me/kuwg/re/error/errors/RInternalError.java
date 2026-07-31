package me.kuwg.re.error.errors;

public class RInternalError extends RuntimeException {
    public RInternalError(final String message) {
        super(message);
    }

    public RInternalError(Throwable t) {
        super(t);
    }

    public RInternalError() {
    }
}
