package me.kuwg.re.operator;

import me.kuwg.re.compiler.CompilationContext;
import me.kuwg.re.type.TypeRef;

public record BinaryOperatorContext(String leftReg, TypeRef leftType, String rightReg, TypeRef rightType,
                                    String fileName, int line, CompilationContext cctx) {
}
