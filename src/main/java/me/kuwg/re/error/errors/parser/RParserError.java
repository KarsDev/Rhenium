package me.kuwg.re.error.errors.parser;

import me.kuwg.re.error.codes.ErrorCodes;
import me.kuwg.re.error.manager.RError;

public final class RParserError extends RError {
    public RParserError(String message, String fileName, int line) {
        super(message + ", in file " + fileName, line);
    }

    @Override
    protected int getCode() {
        return ErrorCodes.PARSER_ERROR;
    }
}
