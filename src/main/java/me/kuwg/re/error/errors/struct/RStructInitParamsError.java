package me.kuwg.re.error.errors.struct;

import me.kuwg.re.error.codes.ErrorCodes;
import me.kuwg.re.error.manager.RError;

public class RStructInitParamsError extends RError {
    public RStructInitParamsError(final String message, final int line) {
        super(message, line);
    }

    @Override
    protected int getCode() {
        return ErrorCodes.STRUCT_INIT_PARAMS_ERROR;
    }
}
