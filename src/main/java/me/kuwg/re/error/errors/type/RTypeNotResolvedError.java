package me.kuwg.re.error.errors.type;

import me.kuwg.re.error.codes.ErrorCodes;
import me.kuwg.re.error.manager.RError;

public class RTypeNotResolvedError extends RError {
    public RTypeNotResolvedError(final String name, final String fileName, final int line) {
        super("Type could not be resolved: " + name, fileName, line);
    }

    @Override
    protected int getCode() {
        return ErrorCodes.TYPE_NOT_RESOLVED_ERROR;
    }
}
