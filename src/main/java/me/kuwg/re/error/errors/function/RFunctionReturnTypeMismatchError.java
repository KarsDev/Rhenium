package me.kuwg.re.error.errors.function;

import me.kuwg.re.error.codes.ErrorCodes;
import me.kuwg.re.error.manager.RError;
import me.kuwg.re.type.TypeRef;

public class RFunctionReturnTypeMismatchError extends RError {
    public RFunctionReturnTypeMismatchError(final TypeRef fnType, final TypeRef retType, final int line) {
        super("Function type mismatch, expected " + fnType.getName() + " and got " + retType.getName(), line);
    }

    @Override
    protected int getCode() {
        return ErrorCodes.FUNCTION_RETURN_TYPE_MISMATCH_ERROR;
    }
}
