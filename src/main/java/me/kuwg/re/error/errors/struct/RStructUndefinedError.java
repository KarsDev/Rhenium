package me.kuwg.re.error.errors.struct;

import me.kuwg.re.error.codes.ErrorCodes;
import me.kuwg.re.error.manager.RError;

public class RStructUndefinedError extends RError {
    public RStructUndefinedError(final String name, final int line) {
        super("Struct not found: " + name, line);
    }

    @Override
    protected int getCode() {
        return ErrorCodes.STRUCT_UNDEFINED_ERROR;
    }
}
