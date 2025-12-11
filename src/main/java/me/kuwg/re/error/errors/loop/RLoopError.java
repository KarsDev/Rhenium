package me.kuwg.re.error.errors.loop;

import me.kuwg.re.error.codes.ErrorCodes;
import me.kuwg.re.error.manager.RError;

public class RLoopError extends RError {
    public RLoopError(String message, int line) {
        super(message, line);
    }

    protected int getCode() {
        return ErrorCodes.LOOP_ERROR;
    }
}
