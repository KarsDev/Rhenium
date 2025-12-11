package me.kuwg.re.error.errors.variable;

import me.kuwg.re.error.codes.ErrorCodes;
import me.kuwg.re.error.manager.RError;

public class RVariableIsNotMutableError extends RError {
    public RVariableIsNotMutableError(final String name, final int line) {
        super("Variable is not mutable: " + name, line);
    }

    @Override
    protected int getCode() {
        return ErrorCodes.VARIABLE_NOT_MUTABLE_ERROR;
    }
}
