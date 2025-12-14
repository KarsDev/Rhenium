package me.kuwg.re.error.errors.expr;

import me.kuwg.re.error.codes.ErrorCodes;
import me.kuwg.re.error.manager.RError;
import me.kuwg.re.type.TypeRef;

public class RUnsupportedUnaryExpressionError extends RError {
    public RUnsupportedUnaryExpressionError(final String op, final TypeRef type, final int line) {
        super("Unsupported type for unary operator: " + op + ", " + type.getName(), line);
    }

    @Override
    protected int getCode() {
        return ErrorCodes.UNSUPPORTED_UNARY_EXPRESSION_ERROR;
    }
}
