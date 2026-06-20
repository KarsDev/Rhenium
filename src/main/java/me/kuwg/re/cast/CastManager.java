package me.kuwg.re.cast;

import me.kuwg.re.ast.types.value.ValueNode;
import me.kuwg.re.compiler.CompilationContext;
import me.kuwg.re.error.errors.cast.RIncompatibleCastError;
import me.kuwg.re.type.TypeRef;
import me.kuwg.re.type.builtin.*;
import me.kuwg.re.type.iterable.arr.ArrayType;
import me.kuwg.re.type.ptr.NullType;
import me.kuwg.re.type.ptr.PointerType;

public final class CastManager {
    public static String executeCast(final String fileName, int line, ValueNode value, TypeRef type, CompilationContext cctx) {
        String valReg = value.compileAndGet(cctx);
        TypeRef from = value.getType();

        cctx.emit("; Cast from " + from.getName() + " to " + type.getName());
        return executeCast(fileName, line, valReg, from, type, cctx);
    }

    public static String executeCast(final String fileName, int line, String valReg, TypeRef from, TypeRef type, CompilationContext cctx) {
        if (from.equals(type)) return valReg;

        if (from instanceof NullType) return fromNull(fileName, line, type, cctx);
        if (from instanceof LongBuiltinType) return fromLong(fileName, line, valReg, type, cctx);
        if (from instanceof IntBuiltinType) return fromInt(fileName, line, valReg, type, cctx);
        if (from instanceof ShortBuiltinType) return fromShort(fileName, line, valReg, type, cctx);
        if (from instanceof ByteBuiltinType) return fromByte(fileName, line, valReg, type, cctx);
        if (from instanceof FloatBuiltinType) return fromFloat(fileName, line, valReg, type, cctx);
        if (from instanceof DoubleBuiltinType) return fromDouble(fileName, line, valReg, type, cctx);
        if (from instanceof BoolBuiltinType) return fromBool(fileName, line, valReg, type, cctx);
        if (from instanceof CharBuiltinType) return fromChar(fileName, line, valReg, type, cctx);
        if (from instanceof AnyPointerType) return fromAnyPointer(fileName, line, valReg, type, cctx);
        if (from instanceof PointerType ptr) return fromPointer(fileName, line, ptr.inner(), valReg, type, cctx);
        if (from instanceof ArrayType arr) return fromArray(fileName, line, arr, valReg, type, cctx);

        return new RIncompatibleCastError(from, type, fileName, line).raise();
    }

    private static String fromNull(final String fileName, int line, TypeRef to, CompilationContext cctx) {
        if (!(to instanceof PointerType || to instanceof AnyPointerType))
            return new RIncompatibleCastError(NullType.INSTANCE, to, fileName, line).raise();
        String result = cctx.nextRegister();
        cctx.emit(result + " = bitcast ptr null to " + to.getLLVMName());
        return result;
    }

    private static String fromLong(final String fileName, int line, String valReg, TypeRef to, CompilationContext cctx) {
        if (to instanceof LongBuiltinType) return valReg;
        String result = cctx.nextRegister();
        if (to instanceof IntBuiltinType) {
            cctx.emit(result + " = trunc i64 " + valReg + " to i32");
            return result;
        }
        if (to instanceof ShortBuiltinType) {
            cctx.emit(result + " = trunc i64 " + valReg + " to i16");
            return result;
        }
        if (to instanceof ByteBuiltinType) {
            cctx.emit(result + " = trunc i64 " + valReg + " to i8");
            return result;
        }
        if (to instanceof FloatBuiltinType) {
            cctx.emit(result + " = sitofp i64 " + valReg + " to float");
            return result;
        }
        if (to instanceof DoubleBuiltinType) {
            cctx.emit(result + " = sitofp i64 " + valReg + " to double");
            return result;
        }
        if (to instanceof AnyPointerType) {
            cctx.emit(result + " = inttoptr i64 " + valReg + " to i8*");
            return result;
        }

        return new RIncompatibleCastError(BuiltinTypes.LONG.getType(), to, fileName, line).raise();
    }

    private static String fromInt(final String fileName, int line, String valReg, TypeRef to, CompilationContext cctx) {
        if (to instanceof IntBuiltinType) return valReg;
        String result = cctx.nextRegister();
        if (to instanceof LongBuiltinType) {
            cctx.emit(result + " = sext i32 " + valReg + " to i64");
            return result;
        }
        if (to instanceof ShortBuiltinType) {
            cctx.emit(result + " = trunc i32 " + valReg + " to i16");
            return result;
        }
        if (to instanceof ByteBuiltinType) {
            cctx.emit(result + " = trunc i32 " + valReg + " to i8");
            return result;
        }
        if (to instanceof FloatBuiltinType) {
            cctx.emit(result + " = sitofp i32 " + valReg + " to float");
            return result;
        }
        if (to instanceof DoubleBuiltinType) {
            cctx.emit(result + " = sitofp i32 " + valReg + " to double");
            return result;
        }
        if (to instanceof AnyPointerType) {
            cctx.emit(result + " = inttoptr i32 " + valReg + " to i8*");
            return result;
        }
        return new RIncompatibleCastError(BuiltinTypes.INT.getType(), to, fileName, line).raise();
    }

    private static String fromShort(final String fileName, int line, String valReg, TypeRef to, CompilationContext cctx) {
        if (to instanceof ShortBuiltinType) return valReg;
        String result = cctx.nextRegister();
        if (to instanceof LongBuiltinType) {
            cctx.emit(result + " = sext i16 " + valReg + " to i64");
            return result;
        }
        if (to instanceof IntBuiltinType) {
            cctx.emit(result + " = sext i16 " + valReg + " to i32");
            return result;
        }
        if (to instanceof ByteBuiltinType) {
            cctx.emit(result + " = trunc i16 " + valReg + " to i8");
            return result;
        }
        if (to instanceof FloatBuiltinType) {
            cctx.emit(result + " = sitofp i16 " + valReg + " to float");
            return result;
        }
        if (to instanceof DoubleBuiltinType) {
            cctx.emit(result + " = sitofp i16 " + valReg + " to double");
            return result;
        }
        return new RIncompatibleCastError(BuiltinTypes.SHORT.getType(), to, fileName, line).raise();
    }

    private static String fromByte(final String fileName, int line, String valReg, TypeRef to, CompilationContext cctx) {
        if (to instanceof ByteBuiltinType) return valReg;
        String result = cctx.nextRegister();
        if (to instanceof ShortBuiltinType) {
            cctx.emit(result + " = sext i8 " + valReg + " to i16");
            return result;
        }
        if (to instanceof IntBuiltinType) {
            cctx.emit(result + " = sext i8 " + valReg + " to i32");
            return result;
        }
        if (to instanceof LongBuiltinType) {
            cctx.emit(result + " = sext i8 " + valReg + " to i64");
            return result;
        }
        if (to instanceof FloatBuiltinType) {
            cctx.emit(result + " = sitofp i8 " + valReg + " to float");
            return result;
        }
        if (to instanceof DoubleBuiltinType) {
            cctx.emit(result + " = sitofp i8 " + valReg + " to double");
            return result;
        }
        return new RIncompatibleCastError(BuiltinTypes.BYTE.getType(), to, fileName, line).raise();
    }

    private static String fromFloat(final String fileName, int line, String valReg, TypeRef to, CompilationContext cctx) {
        if (to instanceof FloatBuiltinType) return valReg;
        String result = cctx.nextRegister();
        if (to instanceof DoubleBuiltinType) {
            cctx.emit(result + " = fpext float " + valReg + " to double");
            return result;
        }
        if (to instanceof LongBuiltinType) {
            cctx.emit(result + " = fptosi float " + valReg + " to i64");
            return result;
        }
        if (to instanceof IntBuiltinType) {
            cctx.emit(result + " = fptosi float " + valReg + " to i32");
            return result;
        }
        if (to instanceof ShortBuiltinType) {
            cctx.emit(result + " = fptosi float " + valReg + " to i16");
            return result;
        }
        if (to instanceof ByteBuiltinType) {
            cctx.emit(result + " = fptosi float " + valReg + " to i8");
            return result;
        }
        return new RIncompatibleCastError(BuiltinTypes.FLOAT.getType(), to, fileName, line).raise();
    }

    private static String fromDouble(final String fileName, int line, String valReg, TypeRef to, CompilationContext cctx) {
        if (to instanceof DoubleBuiltinType) return valReg;
        String result = cctx.nextRegister();
        if (to instanceof FloatBuiltinType) {
            cctx.emit(result + " = fptrunc double " + valReg + " to float");
            return result;
        }
        if (to instanceof LongBuiltinType) {
            cctx.emit(result + " = fptosi double " + valReg + " to i64");
            return result;
        }
        if (to instanceof IntBuiltinType) {
            cctx.emit(result + " = fptosi double " + valReg + " to i32");
            return result;
        }
        if (to instanceof ShortBuiltinType) {
            cctx.emit(result + " = fptosi double " + valReg + " to i16");
            return result;
        }
        if (to instanceof ByteBuiltinType) {
            cctx.emit(result + " = fptosi double " + valReg + " to i8");
            return result;
        }
        return new RIncompatibleCastError(BuiltinTypes.DOUBLE.getType(), to, fileName, line).raise();
    }

    private static String fromBool(final String fileName, int line, String valReg, TypeRef to, CompilationContext cctx) {
        if (to instanceof BoolBuiltinType) return valReg;
        String result = cctx.nextRegister();
        if (to instanceof ByteBuiltinType) {
            cctx.emit(result + " = zext i1 " + valReg + " to i8");
            return result;
        }
        if (to instanceof ShortBuiltinType) {
            cctx.emit(result + " = zext i1 " + valReg + " to i16");
            return result;
        }
        if (to instanceof IntBuiltinType) {
            cctx.emit(result + " = zext i1 " + valReg + " to i32");
            return result;
        }
        if (to instanceof LongBuiltinType) {
            cctx.emit(result + " = zext i1 " + valReg + " to i64");
            return result;
        }
        if (to instanceof FloatBuiltinType) {
            cctx.emit(result + " = uitofp i1 " + valReg + " to float");
            return result;
        }
        if (to instanceof DoubleBuiltinType) {
            cctx.emit(result + " = uitofp i1 " + valReg + " to double");
            return result;
        }
        return new RIncompatibleCastError(BuiltinTypes.BOOL.getType(), to, fileName, line).raise();
    }

    private static String fromChar(final String fileName, int line, String valReg, TypeRef to, CompilationContext cctx) {
        if (to instanceof CharBuiltinType) return valReg;
        if (to instanceof ByteBuiltinType) return valReg;
        String result = cctx.nextRegister();
        if (to instanceof ShortBuiltinType) {
            cctx.emit(result + " = zext i8 " + valReg + " to i16");
            return result;
        }
        if (to instanceof IntBuiltinType) {
            cctx.emit(result + " = zext i8 " + valReg + " to i32");
            return result;
        }
        if (to instanceof LongBuiltinType) {
            cctx.emit(result + " = zext i8 " + valReg + " to i64");
            return result;
        }
        if (to instanceof FloatBuiltinType) {
            cctx.emit(result + " = uitofp i8 " + valReg + " to float");
            return result;
        }
        if (to instanceof DoubleBuiltinType) {
            cctx.emit(result + " = uitofp i8 " + valReg + " to double");
            return result;
        }
        return new RIncompatibleCastError(BuiltinTypes.CHAR.getType(), to, fileName, line).raise();
    }

    private static String fromAnyPointer(final String fileName, int line, String valReg, TypeRef to, CompilationContext cctx) {
        String result = cctx.nextRegister();

        if (to instanceof ArrayType arrType) {
            TypeRef elemType = arrType.inner();
            PointerType ptrToElem = new PointerType(elemType);
            cctx.emit(result + " = bitcast i8* " + valReg + " to " + ptrToElem.getLLVMName());
            return result;
        } else if (!(to instanceof PointerType)) {
            if (to instanceof AnyPointerType) return valReg;
            return new RIncompatibleCastError(BuiltinTypes.ANYPTR.getType(), to, fileName, line).raise();
        }

        cctx.emit(result + " = bitcast i8* " + valReg + " to " + to.getLLVMName());
        return result;

    }

    private static String fromPointer(final String fileName, int line, TypeRef fromInner, String valReg, TypeRef to, CompilationContext cctx) {
        if (to instanceof PointerType toPtr) {
            PointerType fromPtr = new PointerType(fromInner);

            if (fromPtr.getLLVMName().equals(toPtr.getLLVMName())) {
                return valReg;
            }

            String result = cctx.nextRegister();
            cctx.emit(result + " = bitcast " + fromPtr.getLLVMName() + " " + valReg + " to " + toPtr.getLLVMName());
            return result;
        }

        String result = cctx.nextRegister();
        PointerType fromPtr = new PointerType(fromInner);

        if (to instanceof LongBuiltinType) {
            cctx.emit(result + " = ptrtoint " + fromPtr.getLLVMName() + " " + valReg + " to i64");
            return result;
        }

        if (to instanceof AnyPointerType) {
            result = cctx.nextRegister();
            cctx.emit(result + " = bitcast " + fromPtr.getLLVMName() + " " + valReg + " to i8*");
            return result;
        }

        return new RIncompatibleCastError(new PointerType(fromInner), to, fileName, line).raise();
    }

    private static String fromArray(final String fileName, int line, ArrayType from, String valReg, TypeRef to, CompilationContext cctx) {
        if (to instanceof AnyPointerType) {
            String result = cctx.nextRegister();
            PointerType ptrToElem = new PointerType(from.inner());
            cctx.emit(result + " = bitcast " + ptrToElem.getLLVMName() + " " + valReg + " to i8*");
            return result;
        }

        if (to instanceof PointerType toPtr) {
            String result = cctx.nextRegister();
            PointerType ptrToElem = new PointerType(from.inner());
            cctx.emit(result + " = bitcast " + ptrToElem.getLLVMName() + " " + valReg + " to " + toPtr.getLLVMName());
            return result;
        }

        return new RIncompatibleCastError(from, to, fileName, line).raise();
    }
}
