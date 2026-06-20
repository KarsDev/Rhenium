package me.kuwg.re.error.errors.array;

import me.kuwg.re.error.codes.ErrorCodes;
import me.kuwg.re.error.manager.RError;

public class RArrayTypesMismatchError extends RError {
    public RArrayTypesMismatchError(final String expected, final String type, final String fileName, final int line) {
        super(String.format("Array type mismatch: %s given, %s expected", type, expected), fileName, line);
    }

    public RArrayTypesMismatchError(final String fileName, final int line) {
        super("Array type mismatch: expected any type, none given", fileName, line);
    }

    @Override
    protected int getCode() {
        return ErrorCodes.ARRAY_TYPES_MISMATCH_ERROR;
    }
}
