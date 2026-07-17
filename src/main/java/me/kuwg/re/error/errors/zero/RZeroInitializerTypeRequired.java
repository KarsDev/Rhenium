package me.kuwg.re.error.errors.zero;

import me.kuwg.re.error.codes.ErrorCodes;
import me.kuwg.re.error.manager.RError;

public class RZeroInitializerTypeRequired extends RError {
    public RZeroInitializerTypeRequired(final String fileName, final int line) {
        super("Zero initializer without type is only allowed for explicit variable declaration", fileName, line);
    }

    @Override
    protected int getCode() {
        return ErrorCodes.ZERO_INITIALIZER_OUT_OF_SCOPE_ERROR;
    }
}
