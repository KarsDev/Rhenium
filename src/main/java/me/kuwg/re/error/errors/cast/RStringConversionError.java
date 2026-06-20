package me.kuwg.re.error.errors.cast;

import me.kuwg.re.error.codes.ErrorCodes;
import me.kuwg.re.error.manager.RError;

public class RStringConversionError extends RError {
    public RStringConversionError(final String name, final String fileName, final int line) {
        super("Cannot convert " + name + " to string", fileName, line);
    }

    @Override
    protected int getCode() {
        return ErrorCodes.STRING_CONVERSION_ERROR;
    }
}
