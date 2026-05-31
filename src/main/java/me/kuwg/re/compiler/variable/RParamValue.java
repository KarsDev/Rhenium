package me.kuwg.re.compiler.variable;

import me.kuwg.re.ast.types.value.ValueNode;
import org.jetbrains.annotations.Nullable;

public record RParamValue(@Nullable String name, ValueNode value) {
    @Override
    @SuppressWarnings("MethodDoesntCallSuperMethod")
    public RParamValue clone() {
        return new RParamValue(name, value.clone());
    }
}
