package me.kuwg.re.error.errors.constant;

import me.kuwg.re.error.codes.ErrorCodes;
import me.kuwg.re.error.manager.RError;

public class RNotConstantError extends RError {
    public RNotConstantError(final String message, final int line) {
        super(message, line);
    }

    @Override
    protected int getCode() {
        return ErrorCodes.NOT_CONSTANT_ERROR;
    }
}
