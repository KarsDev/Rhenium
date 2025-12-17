package me.kuwg.re.compiler.variable;

import me.kuwg.re.ast.value.ValueNode;
import org.jetbrains.annotations.Nullable;

public record RParamValue(@Nullable String name, ValueNode value) {
}
