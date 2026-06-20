package me.kuwg.re.error.errors.trait;

import me.kuwg.re.error.codes.ErrorCodes;
import me.kuwg.re.error.manager.RError;

public class RTraitAlreadyDeclaredError extends RError {
    public RTraitAlreadyDeclaredError(final String name, final int line, final String fileName) {
        super("Trait is already declared: " + name, fileName, line);
    }

    @Override
    protected int getCode() {
        return ErrorCodes.TRAIT_ALREADY_DECLARED_ERROR;
    }
}
