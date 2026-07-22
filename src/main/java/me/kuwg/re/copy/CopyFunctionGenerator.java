package me.kuwg.re.copy;

import me.kuwg.re.compiler.CompilationContext;
import me.kuwg.re.error.errors.copy.RInvalidCopyTypeError;
import me.kuwg.re.type.TypeRef;
import me.kuwg.re.type.iterable.arr.ArrayType;
import me.kuwg.re.type.ptr.PointerType;
import me.kuwg.re.type.struct.StructType;

import java.util.HashSet;
import java.util.Set;

public final class CopyFunctionGenerator {
    private final CompilationContext cctx;
    private final Set<String> generated = new HashSet<>();

    public CopyFunctionGenerator(CompilationContext cctx) {
        this.cctx = cctx;
    }

    public void ensure(TypeRef type, String fileName, int line) {
        type = cctx.resolveConcrete(type, line);

        if (!generated.add(type.getMangledName())) {
            return;
        }

        if (type.isPrimitive()) {
            return;
        }

        if (type instanceof StructType st) {
            emitStruct(st, line);
        } else if (type instanceof ArrayType at) {
            emitArray(at);
        } else if (type instanceof PointerType pt) {
            emitPointer(pt);
        } else {
            new RInvalidCopyTypeError(type, fileName, line).raise();
        }
    }

    private void emitStruct(StructType st, int line) {
        String fnName = "__copy_" + sanitize(st.getMangledName());
        String llvmType = st.getLLVMName();
        String ptrType = llvmType + "*";

        StringBuilder sb = new StringBuilder();
        sb.append("define void @").append(fnName)
                .append("(").append(ptrType).append(" %dst, ")
                .append(ptrType).append(" %src) {\n")
                .append("entry:\n");

        appendStructCopy(sb, st, "%dst", "%src", "  ", line);

        sb.append("  ret void\n")
                .append("}\n");

        cctx.declare(sb.toString());
    }

    private void appendStructCopy(StringBuilder sb, StructType st, String dstPtr, String srcPtr, String indent, int line) {
        for (int i = 0; i < st.getFieldTypes().size(); i++) {
            TypeRef fieldType = cctx.resolveConcrete(st.getFieldTypes().get(i), line);

            String fieldSrc = "%src_" + i;
            String fieldDst = "%dst_" + i;

            sb.append(indent).append(fieldSrc).append(" = getelementptr inbounds ")
                    .append(st.getLLVMName()).append(", ")
                    .append(st.getLLVMName()).append("* ")
                    .append(srcPtr).append(", i32 0, i32 ").append(i).append('\n');

            sb.append(indent).append(fieldDst).append(" = getelementptr inbounds ")
                    .append(st.getLLVMName()).append(", ")
                    .append(st.getLLVMName()).append("* ")
                    .append(dstPtr).append(", i32 0, i32 ").append(i).append('\n');

            if (fieldType instanceof StructType nested) {
                appendStructCopy(sb, nested, fieldDst, fieldSrc, indent, line);
                continue;
            }

            String loaded = "%val_" + i;
            sb.append(indent).append(loaded).append(" = load ")
                    .append(fieldType.getLLVMName()).append(", ")
                    .append(fieldType.getLLVMName()).append("* ")
                    .append(fieldSrc);

            long alignment = Math.max(1, fieldType.getAlignment());
            if (alignment > 1) {
                sb.append(", align ").append(alignment);
            }
            sb.append('\n');

            sb.append(indent).append("store ").append(fieldType.getLLVMName()).append(" ")
                    .append(loaded).append(", ")
                    .append(fieldType.getLLVMName()).append("* ")
                    .append(fieldDst);
            if (alignment > 1) {
                sb.append(", align ").append(alignment);
            }
            sb.append('\n');
        }
    }

    private void emitArray(ArrayType at) {
        emitCopyFunction(at.getLLVMName(), at.getMangledName());
    }

    private void emitPointer(PointerType pt) {
        emitCopyFunction(pt.getLLVMName(), pt.getMangledName());
    }

    private void emitCopyFunction(String llvmType, String mangledName) {
        String fnName = "__copy_" + sanitize(mangledName);
        String ptrType = llvmType + "*";

        final String sb = "define void @" + fnName +
                "(" + ptrType + " %dst, " +
                ptrType + " %src) {\n" +
                "entry:\n" +
                "  %value = load " + llvmType + ", " +
                ptrType + " %src\n" +
                "  store " + llvmType + " %value, " +
                ptrType + " %dst\n" +
                "  ret void\n" +
                "}\n";

        cctx.declare(sb);
    }

    private static String sanitize(String s) {
        return s.replaceAll("[^A-Za-z0-9_]", "_");
    }
}
