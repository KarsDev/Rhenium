package me.kuwg.re.error.errors.variable;

import me.kuwg.re.error.codes.ErrorCodes;
import me.kuwg.re.error.manager.RError;

public class RVariableAlreadyExistsError extends RError {
    public RVariableAlreadyExistsError(final String name, final int line) {
        super("Variable already exists: " + name, line);
    }

    @Override
    protected int getCode() {
        return ErrorCodes.VARIABLE_ALREADY_EXISTS_ERROR;
    }
}
