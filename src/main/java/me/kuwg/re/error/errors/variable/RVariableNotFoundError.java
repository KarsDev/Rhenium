package me.kuwg.re.error.errors.variable;

import me.kuwg.re.error.codes.ErrorCodes;
import me.kuwg.re.error.manager.RError;

public class RVariableNotFoundError extends RError {
    public RVariableNotFoundError(final String name, final String fileName, final int line) {
        super("Variable not found: " + name, fileName, line);
    }

    public RVariableNotFoundError(final String name, Void ignore, final String fileName, final int line) {
        super("Variable or enum not found: " + name, fileName, line);
    }

    @Override
    protected int getCode() {
        return ErrorCodes.VARIABLE_NOT_FOUND_ERROR;
    }
}
