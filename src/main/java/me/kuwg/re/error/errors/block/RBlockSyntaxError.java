package me.kuwg.re.error.errors.block;

import me.kuwg.re.error.codes.ErrorCodes;
import me.kuwg.re.error.manager.RError;

public class RBlockSyntaxError extends RError {
    public RBlockSyntaxError(final String message, final int line) {
        super(message, line);
    }

    @Override
    protected int getCode() {
        return ErrorCodes.BLOCK_SYNTAX_ERROR;
    }
}
