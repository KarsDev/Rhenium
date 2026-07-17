package me.kuwg.re.error.errors.constructor;

import me.kuwg.re.error.codes.ErrorCodes;
import me.kuwg.re.error.manager.RError;

public class RConstructorOutOfScopeError extends RError {
    public RConstructorOutOfScopeError(final String fileName, final int line) {
        super("Constructor declaration out of scope", fileName, line);
    }

    @Override
    protected int getCode() {
        return ErrorCodes.CONSTRUCTOR_OUT_OF_SCOPE_ERROR;
    }
}
