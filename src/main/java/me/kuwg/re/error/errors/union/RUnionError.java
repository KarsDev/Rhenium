package me.kuwg.re.error.errors.union;

import me.kuwg.re.error.codes.ErrorCodes;
import me.kuwg.re.error.manager.RError;

public class RUnionError extends RError {
    public RUnionError(final String message, final String fileName, final int line) {
        super(message, fileName, line);
    }

    @Override
    protected int getCode() {
        return ErrorCodes.UNION_ERROR;
    }
}
