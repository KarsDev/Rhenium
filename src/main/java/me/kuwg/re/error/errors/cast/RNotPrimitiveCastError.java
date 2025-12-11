package me.kuwg.re.error.errors.cast;

import me.kuwg.re.error.codes.ErrorCodes;
import me.kuwg.re.error.manager.RError;
import me.kuwg.re.type.TypeRef;

public class RNotPrimitiveCastError extends RError {
    public RNotPrimitiveCastError(final TypeRef type, final int line) {
        super("Cannot do a primitive cast for <" + type.getName() + "> since it is not primitive", line);
    }

    @Override
    protected int getCode() {
        return ErrorCodes.NOT_PRIMITIVE_CAST_ERROR;
    }
}
