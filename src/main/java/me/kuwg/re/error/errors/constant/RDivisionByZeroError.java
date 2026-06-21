package me.kuwg.re.error.errors.constant;

import me.kuwg.re.error.codes.ErrorCodes;
import me.kuwg.re.error.manager.RError;

public class RDivisionByZeroError extends RError {
    public RDivisionByZeroError(final String fileName, final int line) {
        super("Division by zero", fileName, line);
    }

    @Override
    protected int getCode() {
        return ErrorCodes.DIVISION_BY_ZERO_ERROR;
    }
}
