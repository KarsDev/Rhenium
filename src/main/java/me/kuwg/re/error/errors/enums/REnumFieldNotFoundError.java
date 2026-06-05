package me.kuwg.re.error.errors.enums;

import me.kuwg.re.error.codes.ErrorCodes;
import me.kuwg.re.error.manager.RError;

public class REnumFieldNotFoundError extends RError {
    public REnumFieldNotFoundError(final String name, final int line) {
        super("Enum field not found: " + name, line);
    }

    @Override
    protected int getCode() {
        return ErrorCodes.EMUM_FIELD_NOT_FOUND;
    }
}
