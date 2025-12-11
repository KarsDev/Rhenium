package me.kuwg.re.error.errors.function;

import me.kuwg.re.error.codes.ErrorCodes;
import me.kuwg.re.error.manager.RError;

public class RFunctionIsVoidError extends RError {
    public RFunctionIsVoidError(final String name, String params, final int line) {
        super("Getting value from a void function: " + name + params, line);
    }

    @Override
    protected int getCode() {
        return ErrorCodes.FUNCTION_IS_VOID_ERROR;
    }
}
