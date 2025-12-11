package me.kuwg.re.error.errors.expr;

import me.kuwg.re.error.codes.ErrorCodes;
import me.kuwg.re.error.manager.RError;

public class RImplNotFunctionError extends RError {
    public RImplNotFunctionError(final int line) {
        super("Impl for struct can only contain function declarations", line);
    }

    @Override
    protected int getCode() {
        return ErrorCodes.IMPL_NOT_FUNCTION_ERROR;
    }
}
