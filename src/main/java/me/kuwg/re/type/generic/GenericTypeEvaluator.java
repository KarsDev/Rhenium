package me.kuwg.re.type.generic;

import me.kuwg.re.compiler.CompilationContext;
import me.kuwg.re.compiler.struct.RGenStruct;
import me.kuwg.re.error.errors.RInternalError;
import me.kuwg.re.type.TypeRef;
import me.kuwg.re.type.iterable.arr.ArrayType;
import me.kuwg.re.type.ptr.PointerType;
import me.kuwg.re.type.struct.AppliedGenStructType;

public interface GenericTypeEvaluator {
    default @SuppressWarnings("unchecked") <T extends TypeRef> T evalType(T type, CompilationContext cctx) {
        if (type instanceof AppliedGenStructType ags) {
            String structName = ags.base().name();
            RGenStruct struct = (RGenStruct) cctx.getStruct(structName);
            type = (T) struct.instantiate(ags.args(), cctx).type();
        } else if (type instanceof PointerType ptr) {
            type = (T) new PointerType(evalType(ptr.inner(), cctx));
        } else if (type instanceof ArrayType arr) {
            type = (T) new ArrayType(arr.size(), evalType(arr.inner(), cctx));
        }

        if (type instanceof AppliedGenStructType) throw new RInternalError();

        return type;
    }
}
