package me.kuwg.re.error.errors.deref;

import me.kuwg.re.error.codes.ErrorCodes;
import me.kuwg.re.error.manager.RError;

public class RNotAddressableError extends RError {
    public RNotAddressableError(final String message, final int line) {
        super(message, line);
    }

    @Override
    protected int getCode() {
        return ErrorCodes.NOT_ADDRESSABLE_ERROR;
    }
}
