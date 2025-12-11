package me.kuwg.re.error.errors.function;

import me.kuwg.re.error.codes.ErrorCodes;
import me.kuwg.re.error.manager.RError;

public class RFunctionNotFoundError extends RError {
    public RFunctionNotFoundError(final String name, String params, final int line) {
        super("Function not found: " + name + params, line);
    }

    @Override
    protected int getCode() {
        return ErrorCodes.FUNCTION_NOT_FOUND_ERROR;
    }
}
