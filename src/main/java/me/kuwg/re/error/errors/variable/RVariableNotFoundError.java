package me.kuwg.re.error.errors.variable;

import me.kuwg.re.error.codes.ErrorCodes;
import me.kuwg.re.error.manager.RError;

public class RVariableNotFoundError extends RError {
    public RVariableNotFoundError(final String name, final int line) {
        super("Variable not found: " + name, line);
    }

    @Override
    protected int getCode() {
        return ErrorCodes.VARIABLE_NOT_FOUND_ERROR;
    }
}
