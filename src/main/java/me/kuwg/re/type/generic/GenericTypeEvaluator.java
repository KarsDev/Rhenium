package me.kuwg.re.type.generic;

import me.kuwg.re.compiler.CompilationContext;
import me.kuwg.re.compiler.struct.RGenStruct;
import me.kuwg.re.error.errors.RInternalError;
import me.kuwg.re.error.errors.trait.RInheritanceError;
import me.kuwg.re.type.TypeRef;
import me.kuwg.re.type.iterable.arr.ArrayType;
import me.kuwg.re.type.ptr.PointerType;
import me.kuwg.re.type.struct.AppliedGenStructType;
import me.kuwg.re.type.trait.TraitType;

public interface GenericTypeEvaluator {
    default @SuppressWarnings("unchecked") <T extends TypeRef> T evalType(T type, CompilationContext cctx, final String fileName, final int line) {
        if (type instanceof AppliedGenStructType ags) {
            String structName = ags.base().name();
            RGenStruct struct = (RGenStruct) cctx.getStruct(structName);
            type = (T) struct.instantiate(ags.args(), cctx).type();
        } else if (type instanceof PointerType ptr) {
            type = (T) new PointerType(evalType(ptr.inner(), cctx, fileName, line));
        } else if (type instanceof ArrayType arr) {
            type = (T) new ArrayType(arr.size(), evalType(arr.inner(), cctx, fileName, line));
        } else if (type instanceof TraitType) {
            return new RInheritanceError("Trait is not usable as a parameter", fileName, line).raise();
        }

        if (type instanceof AppliedGenStructType) throw new RInternalError();

        return type;
    }
}
