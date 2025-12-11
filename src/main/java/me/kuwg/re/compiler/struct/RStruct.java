package me.kuwg.re.compiler.struct;

import me.kuwg.re.compiler.function.RFunction;
import me.kuwg.re.compiler.variable.RStructField;
import me.kuwg.re.type.TypeRef;

import java.util.List;

public record RStruct(TypeRef type, List<RStructField> fields, List<RFunction> functions) {
}
