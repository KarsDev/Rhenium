package me.kuwg.re.error.errors.copy;

import me.kuwg.re.error.codes.ErrorCodes;
import me.kuwg.re.error.manager.RError;
import me.kuwg.re.type.TypeRef;

public class RInvalidCopyTypeError extends RError {
    public RInvalidCopyTypeError(final TypeRef type, final String fileName, final int line) {
        super("Invalid copy type: " + type.getName(), fileName, line);
    }

    @Override
    protected int getCode() {
        return ErrorCodes.INVALID_COPY_TYPE_ERROR;
    }
}
