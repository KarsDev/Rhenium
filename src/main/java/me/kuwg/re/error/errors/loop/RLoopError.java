package me.kuwg.re.error.errors.loop;

import me.kuwg.re.error.codes.ErrorCodes;
import me.kuwg.re.error.manager.RError;

public class RLoopError extends RError {
    public RLoopError(String message, final String fileName, int line) {
        super(message, fileName, line);
    }

    protected int getCode() {
        return ErrorCodes.LOOP_ERROR;
    }
}
