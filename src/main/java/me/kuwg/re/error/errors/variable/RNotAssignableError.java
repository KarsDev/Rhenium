package me.kuwg.re.error.errors.variable;

import me.kuwg.re.error.codes.ErrorCodes;
import me.kuwg.re.error.manager.RError;

public @SuppressWarnings("unused") class RNotAssignableError extends RError {
    public RNotAssignableError(final String fileName, final int line) {
        super("Expression is not assignable", fileName, line);
    }

    @Override
    protected int getCode() {
        return ErrorCodes.NOT_ASSIGNABLE_ERROR;
    }
}
