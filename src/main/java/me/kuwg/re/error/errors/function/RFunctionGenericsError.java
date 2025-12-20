package me.kuwg.re.error.errors.function;

import me.kuwg.re.error.codes.ErrorCodes;
import me.kuwg.re.error.manager.RError;

public class RFunctionGenericsError extends RError {
    public RFunctionGenericsError(final String message, final int line) {
        super(message, line);
    }

    @Override
    protected int getCode() {
        return ErrorCodes.FUNCTION_GENERICS_ERROR;
    }
}
