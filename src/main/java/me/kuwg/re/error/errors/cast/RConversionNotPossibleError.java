package me.kuwg.re.error.errors.cast;

import me.kuwg.re.error.codes.ErrorCodes;
import me.kuwg.re.error.manager.RError;
import me.kuwg.re.type.TypeRef;

public @SuppressWarnings("unused") class RConversionNotPossibleError extends RError {
    public RConversionNotPossibleError(final TypeRef type, final int line) {
        super("Conversion is not supported for " + type.getName(), line);
    }

    @Override
    protected int getCode() {
        return ErrorCodes.CONVERSION_NOT_POSSIBLE_ERROR;
    }
}
