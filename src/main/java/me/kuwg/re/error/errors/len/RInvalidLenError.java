package me.kuwg.re.error.errors.len;

import me.kuwg.re.error.codes.ErrorCodes;
import me.kuwg.re.error.manager.RError;

public class RInvalidLenError extends RError {
    public RInvalidLenError(final String type, final int line) {
        super("Cannot take len() of type: " + type, line);
    }

    @Override
    protected int getCode() {
        return ErrorCodes.INVALID_LEN_ERROR;
    }
}
