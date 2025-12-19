package me.kuwg.re.error.errors.deref;

import me.kuwg.re.error.codes.ErrorCodes;
import me.kuwg.re.error.manager.RError;

public class RDerefAnyPointerError extends RError {
    public RDerefAnyPointerError(final int line) {
        super("Dereferencing a anyptr, please cast before using the dereference operator", line);
    }

    @Override
    protected int getCode() {
        return ErrorCodes.DEREFERENCE_ANYPTR_ERROR;
    }
}
