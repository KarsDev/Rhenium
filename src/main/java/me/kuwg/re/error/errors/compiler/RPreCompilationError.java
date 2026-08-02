package me.kuwg.re.error.errors.compiler;

import me.kuwg.re.error.codes.ErrorCodes;
import me.kuwg.re.error.manager.RError;

public class RPreCompilationError extends RError {
    public RPreCompilationError(final String message) {
        super(message, null, -1);
    }

    @Override
    protected int getCode() {
        return ErrorCodes.PRE_COMPILATION_ERROR;
    }
}
