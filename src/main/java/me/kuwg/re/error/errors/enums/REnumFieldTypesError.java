package me.kuwg.re.error.errors.enums;

import me.kuwg.re.error.codes.ErrorCodes;
import me.kuwg.re.error.manager.RError;

public class REnumFieldTypesError extends RError {
    public REnumFieldTypesError(final String message, final int line) {
        super(message, line);
    }

    @Override
    protected int getCode() {
        return ErrorCodes.ENUM_FIELD_TYPES_ERROR;
    }
}
