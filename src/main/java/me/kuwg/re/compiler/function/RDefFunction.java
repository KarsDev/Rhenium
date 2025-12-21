package me.kuwg.re.compiler.function;

import me.kuwg.re.ast.nodes.function.declaration.FunctionParameter;
import me.kuwg.re.type.TypeRef;

import java.util.List;

public class RDefFunction extends RFunction {
    public RDefFunction(final String llvmName, final String name, final TypeRef returnType,
                        final List<FunctionParameter> parameters) {
        super(llvmName, name, returnType, parameters);
    }
}
