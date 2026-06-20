package me.kuwg.re.error.errors.condition;

import me.kuwg.re.error.codes.ErrorCodes;
import me.kuwg.re.error.manager.RError;
import me.kuwg.re.type.TypeRef;

public class RInvalidConditionError extends RError {
    public RInvalidConditionError(final TypeRef type, final String fileName, final int line) {
        super("Invalid condition, expected bool but got " + type.getName(), fileName, line);
    }

    @Override
    protected int getCode() {
        return ErrorCodes.INVALID_CONDITION_ERROR;
    }
}
