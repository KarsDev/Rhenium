package me.kuwg.re.error.errors.ternary;

import me.kuwg.re.error.codes.ErrorCodes;
import me.kuwg.re.error.manager.RError;

public class RTernaryOperatorError extends RError {
    public RTernaryOperatorError(final String message, final String fileName, final int line) {
        super(message, fileName, line);
    }

    @Override
    protected int getCode() {
        return ErrorCodes.TERNARY_OPERATOR_ERROR;
    }
}
