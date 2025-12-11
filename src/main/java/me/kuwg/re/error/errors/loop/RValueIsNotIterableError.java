package me.kuwg.re.error.errors.loop;

import me.kuwg.re.error.codes.ErrorCodes;
import me.kuwg.re.error.manager.RError;

public class RValueIsNotIterableError extends RError {
    public RValueIsNotIterableError(final String name, final int line) {
        super("Value is not iterable: "+ name, line);
    }

    @Override
    protected int getCode() {
        return ErrorCodes.VALUE_IS_NOT_ITERABLE_ERROR;
    }
}
