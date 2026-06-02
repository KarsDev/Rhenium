package me.kuwg.re.error.errors.range;

import me.kuwg.re.error.codes.ErrorCodes;
import me.kuwg.re.error.manager.RError;

public class RRangeTypeError extends RError {
    public RRangeTypeError(final int line) {
        super("Range type is not supported for variables or functions", line);
    }

    @Override
    protected int getCode() {
        return ErrorCodes.RANGE_TYPE_ERROR;
    }
}
