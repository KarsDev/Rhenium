package me.kuwg.re.error.errors.array;

import me.kuwg.re.error.codes.ErrorCodes;
import me.kuwg.re.error.manager.RError;

public class RArrayTypeIsNoneError extends RError {
    public RArrayTypeIsNoneError(final int line) {
        super("Array inner type is none", line);
    }

    @Override
    protected int getCode() {
        return ErrorCodes.ARRAY_TYPE_IS_NONE_ERROR;
    }
}
