package me.kuwg.re.error.errors;

public class RInternalError extends RuntimeException {
    public RInternalError(final String message, final Throwable cause) {
        super(message, cause);
    }

    public RInternalError(final String message) {
        super(message);
    }

    public RInternalError(final Throwable cause) {
        super(cause);
    }

    public RInternalError() {
    }
}
