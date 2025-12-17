package me.kuwg.re.error.errors.natv;

import me.kuwg.re.error.codes.ErrorCodes;
import me.kuwg.re.error.manager.RError;

public class RNativeCPPError extends RError {
    public RNativeCPPError(final String message, final int line) {
        super(message, line);
    }

    @Override
    protected int getCode() {
        return ErrorCodes.NATIVE_CPP_ERROR;
    }
}
