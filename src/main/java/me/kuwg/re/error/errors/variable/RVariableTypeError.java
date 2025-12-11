package me.kuwg.re.error.errors.variable;

import me.kuwg.re.error.codes.ErrorCodes;
import me.kuwg.re.error.manager.RError;

public class RVariableTypeError extends RError {
    public RVariableTypeError(final String valueType, String declaredType, final int line) {
        super(String.format("Variable value type (%s) is not compatible with declared type (%s)", valueType, declaredType), line);
    }

    @Override
    protected int getCode() {
        return ErrorCodes.VARIABLE_TYPE_ERROR;
    }
}
