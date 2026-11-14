package me.kuwg.re.error.errors.number;

import me.kuwg.re.error.codes.ErrorCodes;
import me.kuwg.re.error.manager.RError;

public class RNumberFormatError extends RError {
    public RNumberFormatError(final String message, final String fileName, final int line) {
        super(message, fileName, line);
    }

    @Override
    protected int getCode() {
        return ErrorCodes.NUMBER_FORMAT_ERROR;
    }
}
