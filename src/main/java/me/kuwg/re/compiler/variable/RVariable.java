package me.kuwg.re.compiler.variable;

import me.kuwg.re.type.TypeRef;

public record RVariable(String name, boolean mutable, TypeRef type, String valueReg) {
}
