package me.kuwg.re.error.errors.module;

import me.kuwg.re.error.codes.ErrorCodes;
import me.kuwg.re.error.manager.RError;

public class RModuleCouldNotBeLoadedError extends RError {
    public RModuleCouldNotBeLoadedError(final String name, final int line) {
        super("Module could not be loaded found: " + name, line);
    }

    @Override
    protected int getCode() {
        return ErrorCodes.MODULE_NOT_LOADED_FOUND_ERROR;
    }
}
