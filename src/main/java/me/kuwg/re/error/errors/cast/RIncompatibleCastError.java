package me.kuwg.re.error.errors.cast;

import me.kuwg.re.error.codes.ErrorCodes;
import me.kuwg.re.error.manager.RError;
import me.kuwg.re.type.TypeRef;

public class RIncompatibleCastError extends RError {
    public RIncompatibleCastError(final TypeRef from, final TypeRef to, final int line) {
        super("Incompatible cast from " + from.getName() + " to " + to.getName(), line);
    }

    @Override
    protected int getCode() {
        return ErrorCodes.INCOMPATIBLE_CAST_ERROR;
    }
}
