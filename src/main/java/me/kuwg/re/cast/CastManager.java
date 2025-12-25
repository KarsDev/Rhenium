package me.kuwg.re.cast;

import me.kuwg.re.ast.types.value.ValueNode;
import me.kuwg.re.compiler.CompilationContext;
import me.kuwg.re.error.errors.cast.RIncompatibleCastError;
import me.kuwg.re.type.TypeRef;
import me.kuwg.re.type.builtin.*;
import me.kuwg.re.type.ptr.NullType;
import me.kuwg.re.type.ptr.PointerType;

public final class CastManager {
    public static String executeCast(int line, ValueNode value, TypeRef type, CompilationContext cctx) {
        String valReg = value.compileAndGet(cctx);
        TypeRef from = value.getType();

        return executeCast(line, valReg, from, type, cctx);
    }

    public static String executeCast(int line, String valReg, TypeRef from, TypeRef type, CompilationContext cctx) {
        if (from instanceof NullType) return fromNull(line, type, cctx);
        if (from instanceof LongBuiltinType) return fromLong(line, valReg, type, cctx);
        if (from instanceof IntBuiltinType) return fromInt(line, valReg, type, cctx);
        if (from instanceof ShortBuiltinType) return fromShort(line, valReg, type, cctx);
        if (from instanceof ByteBuiltinType) return fromByte(line, valReg, type, cctx);
        if (from instanceof FloatBuiltinType) return fromFloat(line, valReg, type, cctx);
        if (from instanceof DoubleBuiltinType) return fromDouble(line, valReg, type, cctx);
        if (from instanceof BoolBuiltinType) return fromBool(line, valReg, type, cctx);
        if (from instanceof CharBuiltinType) return fromChar(line, valReg, type, cctx);
        if (from instanceof AnyPointerType) return fromAnyPointer(line, valReg, type, cctx);
        if (from instanceof PointerType ptr) return fromPointer(line, ptr.inner(), valReg, type, cctx);

        if (from.equals(type)) return valReg;

        return new RIncompatibleCastError(from, type, line).raise();
    }


    private static String fromNull(int line, TypeRef to, CompilationContext cctx) {
        if (!(to instanceof PointerType)) return new RIncompatibleCastError(NullType.INSTANCE, to, line).raise();
        String result = cctx.nextRegister();
        cctx.emit(result + " = bitcast ptr null to " + to.getLLVMName());
        return result;
    }

    private static String fromLong(int line, String valReg, TypeRef to, CompilationContext cctx) {
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
        return new RIncompatibleCastError(BuiltinTypes.LONG.getType(), to, line).raise();
    }

    private static String fromInt(int line, String valReg, TypeRef to, CompilationContext cctx) {
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
        return new RIncompatibleCastError(BuiltinTypes.INT.getType(), to, line).raise();
    }

    private static String fromShort(int line, String valReg, TypeRef to, CompilationContext cctx) {
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
        return new RIncompatibleCastError(BuiltinTypes.SHORT.getType(), to, line).raise();
    }

    private static String fromByte(int line, String valReg, TypeRef to, CompilationContext cctx) {
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
        return new RIncompatibleCastError(BuiltinTypes.BYTE.getType(), to, line).raise();
    }

    private static String fromFloat(int line, String valReg, TypeRef to, CompilationContext cctx) {
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
        return new RIncompatibleCastError(BuiltinTypes.FLOAT.getType(), to, line).raise();
    }

    private static String fromDouble(int line, String valReg, TypeRef to, CompilationContext cctx) {
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
        return new RIncompatibleCastError(BuiltinTypes.DOUBLE.getType(), to, line).raise();
    }

    private static String fromBool(int line, String valReg, TypeRef to, CompilationContext cctx) {
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
        return new RIncompatibleCastError(BuiltinTypes.BOOL.getType(), to, line).raise();
    }

    private static String fromChar(int line, String valReg, TypeRef to, CompilationContext cctx) {
        if (to instanceof CharBuiltinType) return valReg;
        String result = cctx.nextRegister();
        if (to instanceof ByteBuiltinType) {
            cctx.emit(result + " = trunc i8 " + valReg + " to i8");
            return result;
        }
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
        return new RIncompatibleCastError(BuiltinTypes.CHAR.getType(), to, line).raise();
    }

    private static String fromAnyPointer(int line, String valReg, TypeRef to, CompilationContext cctx) {
        if (!(to instanceof PointerType)) {
            if (to instanceof AnyPointerType) return valReg;
            return new RIncompatibleCastError(BuiltinTypes.ANYPTR.getType(), to, line).raise();
        }

        String result = cctx.nextRegister();
        cctx.emit(result + " = bitcast i8* " + valReg + " to " + to.getLLVMName());
        return result;

    }

    private static String fromPointer(int line, TypeRef fromInner, String valReg, TypeRef to, CompilationContext cctx) {
        if (to instanceof PointerType toPtr) {
            PointerType fromPtr = new PointerType(fromInner);

            if (fromPtr.getLLVMName().equals(toPtr.getLLVMName())) {
                return valReg;
            }

            String result = cctx.nextRegister();
            cctx.emit(result + " = bitcast " + fromPtr.getLLVMName() + " " + valReg + " to " + toPtr.getLLVMName());
            return result;
        }

        if (!(to instanceof AnyPointerType)) {
            return new RIncompatibleCastError(new PointerType(fromInner), to, line).raise();
        }
        String result = cctx.nextRegister();
        cctx.emit(result + " = bitcast " + new PointerType(fromInner).getLLVMName() + " " + valReg + " to i8*");
        return result;

    }
}
