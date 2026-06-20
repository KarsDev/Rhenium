package me.kuwg.re.error.errors.expr;

import me.kuwg.re.error.codes.ErrorCodes;
import me.kuwg.re.error.manager.RError;

public class RUnsupportedBinaryExpressionError extends RError {
    public RUnsupportedBinaryExpressionError(String left, String op, String right, final String fileName, final int line) {
        super(String.format("Unsupported binary expression: %s %s %s", left, op, right), fileName, line);
    }

    @Override
    protected int getCode() {
        return ErrorCodes.UNSUPPORTED_BINARY_EXPRESSION_ERROR;
    }
}
