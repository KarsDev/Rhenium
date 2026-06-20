package me.kuwg.re.error.errors.enums;

import me.kuwg.re.error.codes.ErrorCodes;
import me.kuwg.re.error.manager.RError;

public class REnumIsAlreadyDeclaredError extends RError {
    public REnumIsAlreadyDeclaredError(final String name, final String fileName, final int line) {
        super("Enum is already declared: " + name, fileName, line);
    }

    @Override
    protected int getCode() {
        return ErrorCodes.ENUM_ALREADY_DECLARED_ERROR;
    }
}
