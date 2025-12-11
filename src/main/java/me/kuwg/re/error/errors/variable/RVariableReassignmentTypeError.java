package me.kuwg.re.error.errors.variable;

import me.kuwg.re.error.codes.ErrorCodes;
import me.kuwg.re.error.manager.RError;

public class RVariableReassignmentTypeError extends RError {
    public RVariableReassignmentTypeError(final String name, final int line) {
        super("You can't specify a type in variable reassignment: " + name, line);
    }

    @Override
    protected int getCode() {
        return ErrorCodes.VARIABLE_REASSIGNMENT_TYPE_ERROR;
    }
}
