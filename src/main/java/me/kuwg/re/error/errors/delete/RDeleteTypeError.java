package me.kuwg.re.error.errors.delete;

import me.kuwg.re.error.codes.ErrorCodes;
import me.kuwg.re.error.manager.RError;

public class RDeleteTypeError extends RError {
    public RDeleteTypeError(final String message, final String fileName, final int line) {
        super(message, fileName, line);
    }

    @Override
    protected int getCode() {
        return ErrorCodes.DELETE_TYPE_ERROR;
    }
}
