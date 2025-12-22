package me.kuwg.re.error.errors.struct;

import me.kuwg.re.error.codes.ErrorCodes;
import me.kuwg.re.error.manager.RError;

public class RStructAlreadyExistsError extends RError {
    public RStructAlreadyExistsError(final String name, final int line) {
        super("Struct already exists: " + name, line);
    }

    @Override
    protected int getCode() {
        return ErrorCodes.STRUCT_ALREADY_EXISTS_ERROR;
    }
}
