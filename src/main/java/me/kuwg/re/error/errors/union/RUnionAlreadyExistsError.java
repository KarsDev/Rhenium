package me.kuwg.re.error.errors.union;

import me.kuwg.re.error.codes.ErrorCodes;
import me.kuwg.re.error.manager.RError;

public class RUnionAlreadyExistsError extends RError {
    public RUnionAlreadyExistsError(final String name, final String fileName, final int line) {
        super("Union already exists: " + name, fileName, line);
    }

    @Override
    protected int getCode() {
        return ErrorCodes.STRUCT_ALREADY_EXISTS_ERROR;
    }
}
