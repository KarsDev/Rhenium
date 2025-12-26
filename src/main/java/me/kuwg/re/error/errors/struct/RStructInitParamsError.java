package me.kuwg.re.error.errors.struct;

import me.kuwg.re.error.codes.ErrorCodes;
import me.kuwg.re.error.manager.RError;

public class RStructInitParamsError extends RError {
    public RStructInitParamsError(final String name, final int given, final int provided, final int line) {
        super("Expected " + given + " parameters but got " + provided + " for struct " + name, line);
    }
    @Override
    protected int getCode() {
        return ErrorCodes.STRUCT_INIT_PARAMS_ERROR;
    }
}
