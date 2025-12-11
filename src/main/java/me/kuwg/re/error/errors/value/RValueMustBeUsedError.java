package me.kuwg.re.error.errors.value;

import me.kuwg.re.error.codes.ErrorCodes;
import me.kuwg.re.error.manager.RError;

public class RValueMustBeUsedError extends RError {
    public RValueMustBeUsedError(final String name, final int line) {
        super(name + " values must be used", line);
    }

    @Override
    protected int getCode() {
        return ErrorCodes.VALUE_MUST_BE_USED_ERROR;
    }
}
