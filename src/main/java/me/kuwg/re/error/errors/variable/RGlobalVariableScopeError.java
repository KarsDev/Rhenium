package me.kuwg.re.error.errors.variable;

import me.kuwg.re.error.codes.ErrorCodes;
import me.kuwg.re.error.manager.RError;

public class RGlobalVariableScopeError extends RError {
    public RGlobalVariableScopeError(final String name, final String fileName, final int line) {
        super("Global variables can only be defined outside of a function: " + name, fileName, line);
    }

    @Override
    protected int getCode() {
        return ErrorCodes.GLOBAL_VARIABLE_SCOPE_ERROR;
    }
}
