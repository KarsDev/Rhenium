package me.kuwg.re.error.errors.destructor;

import me.kuwg.re.error.codes.ErrorCodes;
import me.kuwg.re.error.manager.RError;

public class RDestructorOutOfScopeError extends RError {
    public RDestructorOutOfScopeError(final String fileName, final int line) {
        super("Destructor out of scope", fileName, line);
    }

    @Override
    protected int getCode() {
        return ErrorCodes.DESTRUCTOR_OUT_OF_SCOPE_ERROR;
    }
}
