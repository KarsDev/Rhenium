package me.kuwg.re.error.errors.module;

import me.kuwg.re.error.codes.ErrorCodes;
import me.kuwg.re.error.manager.RError;

public class RModuleNotFoundError extends RError {
    public RModuleNotFoundError(final String name, final int line) {
        super("Module not found: " + name, line);
    }

    @Override
    protected int getCode() {
        return ErrorCodes.MODULE_NOT_FOUND_ERROR;
    }
}
