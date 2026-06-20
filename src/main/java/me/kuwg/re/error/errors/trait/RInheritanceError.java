package me.kuwg.re.error.errors.trait;

import me.kuwg.re.error.codes.ErrorCodes;
import me.kuwg.re.error.manager.RError;

public class RInheritanceError extends RError {
    public RInheritanceError(final String message, final String fileName, final int line) {
        super(message, fileName, line);
    }

    @Override
    protected int getCode() {
        return ErrorCodes.INHERITANCE_ERROR;
    }
}
