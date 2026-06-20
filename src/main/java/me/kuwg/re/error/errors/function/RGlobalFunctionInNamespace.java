package me.kuwg.re.error.errors.function;

import me.kuwg.re.error.codes.ErrorCodes;
import me.kuwg.re.error.manager.RError;

public class RGlobalFunctionInNamespace extends RError {
    public RGlobalFunctionInNamespace(final String fileName, final int line) {
        super("Global functions can't be declared in namespaces", fileName, line);
    }

    @Override
    protected int getCode() {
        return ErrorCodes.GLOBAL_FUNCTION_IN_NAMESPACE_ERROR;
    }
}
