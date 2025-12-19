package me.kuwg.re.compiler.variable;

import me.kuwg.re.ast.types.value.ValueNode;
import me.kuwg.re.type.TypeRef;
import org.jetbrains.annotations.Nullable;

public record RStructField(String name, TypeRef type, @Nullable ValueNode defaultValue) {
}
