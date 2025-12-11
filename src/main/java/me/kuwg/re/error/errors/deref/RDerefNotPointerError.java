package me.kuwg.re.error.errors.deref;

import me.kuwg.re.error.codes.ErrorCodes;
import me.kuwg.re.error.manager.RError;

public class RDerefNotPointerError extends RError {
    public RDerefNotPointerError(final String name, final int line) {
        super("Dereferencing a variable that is not a pointer: " + name, line);
    }

    @Override
    protected int getCode() {
        return ErrorCodes.DEREF_NOT_POINTER_ERROR;
    }
}
