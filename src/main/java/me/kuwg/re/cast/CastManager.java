package me.kuwg.re.cast;

import me.kuwg.re.ast.types.value.ValueNode;
import me.kuwg.re.compiler.CompilationContext;
import me.kuwg.re.error.errors.cast.RIncompatibleCastError;
import me.kuwg.re.type.TypeRef;
import me.kuwg.re.type.builtin.*;
import me.kuwg.re.type.iterable.arr.ArrayType;
import me.kuwg.re.type.ptr.NullType;
import me.kuwg.re.type.ptr.PointerType;
import me.kuwg.re.type.struct.StructType;
import me.kuwg.re.type.union.UnionType;

public final class CastManager {

    private CastManager() {}

    public static String executeCast(final String fileName,
                                     final int line,
                                     final ValueNode value,
                                     final TypeRef targetType,
                                     final CompilationContext cctx) {
        final String valueRegister = value.compileAndGet(cctx);
        final TypeRef sourceType = value.getType();

        cctx.emit("; Cast from " + sourceType.getName() + " to " + targetType.getName());
        return executeCast(fileName, line, valueRegister, sourceType, targetType, cctx);
    }

    public static String executeCast(final String fileName,
                                     final int line,
                                     final String valueRegister,
                                     final TypeRef sourceType,
                                     final TypeRef targetType,
                                     final CompilationContext cctx) {
        if (sourceType.equals(targetType)) {
            return valueRegister;
        }

        if (sourceType instanceof NullType) {
            return fromNull(fileName, line, targetType, cctx);
        }
        if (sourceType instanceof LongBuiltinType) {
            return fromLong(fileName, line, valueRegister, targetType, cctx);
        }
        if (sourceType instanceof IntBuiltinType) {
            return fromInt(fileName, line, valueRegister, targetType, cctx);
        }
        if (sourceType instanceof ShortBuiltinType) {
            return fromShort(fileName, line, valueRegister, targetType, cctx);
        }
        if (sourceType instanceof ByteBuiltinType) {
            return fromByte(fileName, line, valueRegister, targetType, cctx);
        }
        if (sourceType instanceof FloatBuiltinType) {
            return fromFloat(fileName, line, valueRegister, targetType, cctx);
        }
        if (sourceType instanceof DoubleBuiltinType) {
            return fromDouble(fileName, line, valueRegister, targetType, cctx);
        }
        if (sourceType instanceof BoolBuiltinType) {
            return fromBool(fileName, line, valueRegister, targetType, cctx);
        }
        if (sourceType instanceof CharBuiltinType) {
            return fromChar(fileName, line, valueRegister, targetType, cctx);
        }
        if (sourceType instanceof AnyPointerType) {
            return fromAnyPointer(fileName, line, valueRegister, targetType, cctx);
        }
        if (sourceType instanceof PointerType pointerType) {
            return fromPointer(fileName, line, pointerType.getInner(), valueRegister, targetType, cctx);
        }
        if (sourceType instanceof ArrayType arrayType) {
            return fromArray(fileName, line, arrayType, valueRegister, targetType, cctx);
        }
        if (sourceType instanceof StrBuiltinType) {
            return fromStr(fileName, line, valueRegister, targetType, cctx);
        }
        if (sourceType instanceof UnionType unionType) {
            return fromUnion(fileName, line, unionType, valueRegister, targetType, cctx);
        }
        if (sourceType instanceof StructType structType) {
            return fromStruct(fileName, line, structType, valueRegister, targetType, cctx);
        }

        return incompatible(fileName, line, sourceType, targetType);
    }

    private static String incompatible(final String fileName,
                                       final int line,
                                       final TypeRef from,
                                       final TypeRef to) {
        return new RIncompatibleCastError(from, to, fileName, line).raise();
    }

    private static String newRegister(final CompilationContext cctx) {
        return cctx.nextRegister();
    }

    private static String emitAndReturn(final CompilationContext cctx, final String instruction) {
        final String result = newRegister(cctx);
        cctx.emit(result + " = " + instruction);
        return result;
    }

    private static String fromNull(final String fileName, final int line, final TypeRef targetType, final CompilationContext cctx) {
        if (!(targetType instanceof PointerType || targetType instanceof AnyPointerType || targetType instanceof StrBuiltinType)) {
            return incompatible(fileName, line, NullType.INSTANCE, targetType);
        }

        final String result = newRegister(cctx);
        cctx.emit(result + " = bitcast ptr null to " + targetType.getLLVMName());
        return result;
    }

    private static String fromLong(final String fileName,
                                   final int line,
                                   final String valueRegister,
                                   final TypeRef targetType,
                                   final CompilationContext cctx) {
        if (targetType instanceof LongBuiltinType) return valueRegister;

        if (targetType instanceof IntBuiltinType || targetType instanceof ShortBuiltinType || targetType instanceof ByteBuiltinType) {
            return emitAndReturn(cctx, "trunc i64 " + valueRegister + " to " + targetType.getLLVMName());
        }
        if (targetType instanceof FloatBuiltinType || targetType instanceof DoubleBuiltinType) {
            return emitAndReturn(cctx, "sitofp i64 " + valueRegister + " to " + targetType.getLLVMName());
        }
        if (targetType instanceof AnyPointerType) {
            return emitAndReturn(cctx, "inttoptr i64 " + valueRegister + " to i8*");
        }

        return incompatible(fileName, line, BuiltinTypes.LONG.getType(), targetType);
    }

    private static String fromInt(final String fileName,
                                  final int line,
                                  final String valueRegister,
                                  final TypeRef targetType,
                                  final CompilationContext cctx) {
        if (targetType instanceof IntBuiltinType) return valueRegister;

        if (targetType instanceof LongBuiltinType || targetType instanceof ShortBuiltinType || targetType instanceof ByteBuiltinType) {
            return emitAndReturn(cctx, "sext i32 " + valueRegister + " to " + targetType.getLLVMName());
        }
        if (targetType instanceof FloatBuiltinType || targetType instanceof DoubleBuiltinType) {
            return emitAndReturn(cctx, "sitofp i32 " + valueRegister + " to " + targetType.getLLVMName());
        }
        if (targetType instanceof AnyPointerType) {
            return emitAndReturn(cctx, "inttoptr i32 " + valueRegister + " to i8*");
        }

        return incompatible(fileName, line, BuiltinTypes.INT.getType(), targetType);
    }

    private static String fromShort(final String fileName,
                                    final int line,
                                    final String valueRegister,
                                    final TypeRef targetType,
                                    final CompilationContext cctx) {
        if (targetType instanceof ShortBuiltinType) return valueRegister;

        if (targetType instanceof LongBuiltinType || targetType instanceof IntBuiltinType || targetType instanceof ByteBuiltinType) {
            return emitAndReturn(cctx, "sext i16 " + valueRegister + " to " + targetType.getLLVMName());
        }
        if (targetType instanceof FloatBuiltinType || targetType instanceof DoubleBuiltinType) {
            return emitAndReturn(cctx, "sitofp i16 " + valueRegister + " to " + targetType.getLLVMName());
        }

        return incompatible(fileName, line, BuiltinTypes.SHORT.getType(), targetType);
    }

    private static String fromByte(final String fileName,
                                   final int line,
                                   final String valueRegister,
                                   final TypeRef targetType,
                                   final CompilationContext cctx) {
        if (targetType instanceof ByteBuiltinType) return valueRegister;

        if (targetType instanceof ShortBuiltinType || targetType instanceof IntBuiltinType || targetType instanceof LongBuiltinType) {
            return emitAndReturn(cctx, "sext i8 " + valueRegister + " to " + targetType.getLLVMName());
        }
        if (targetType instanceof FloatBuiltinType || targetType instanceof DoubleBuiltinType) {
            return emitAndReturn(cctx, "sitofp i8 " + valueRegister + " to " + targetType.getLLVMName());
        }

        return incompatible(fileName, line, BuiltinTypes.BYTE.getType(), targetType);
    }

    private static String fromFloat(final String fileName,
                                    final int line,
                                    final String valueRegister,
                                    final TypeRef targetType,
                                    final CompilationContext cctx) {
        if (targetType instanceof FloatBuiltinType) return valueRegister;

        if (targetType instanceof DoubleBuiltinType) {
            return emitAndReturn(cctx, "fpext float " + valueRegister + " to double");
        }
        if (targetType instanceof LongBuiltinType || targetType instanceof IntBuiltinType ||
                targetType instanceof ShortBuiltinType || targetType instanceof ByteBuiltinType) {
            return emitAndReturn(cctx, "fptosi float " + valueRegister + " to " + targetType.getLLVMName());
        }

        return incompatible(fileName, line, BuiltinTypes.FLOAT.getType(), targetType);
    }

    private static String fromDouble(final String fileName,
                                     final int line,
                                     final String valueRegister,
                                     final TypeRef targetType,
                                     final CompilationContext cctx) {
        if (targetType instanceof DoubleBuiltinType) return valueRegister;

        if (targetType instanceof FloatBuiltinType) {
            return emitAndReturn(cctx, "fptrunc double " + valueRegister + " to float");
        }
        if (targetType instanceof LongBuiltinType || targetType instanceof IntBuiltinType ||
                targetType instanceof ShortBuiltinType || targetType instanceof ByteBuiltinType) {
            return emitAndReturn(cctx, "fptosi double " + valueRegister + " to " + targetType.getLLVMName());
        }

        return incompatible(fileName, line, BuiltinTypes.DOUBLE.getType(), targetType);
    }

    private static String fromBool(final String fileName,
                                   final int line,
                                   final String valueRegister,
                                   final TypeRef targetType,
                                   final CompilationContext cctx) {
        if (targetType instanceof BoolBuiltinType) return valueRegister;

        if (targetType instanceof ByteBuiltinType || targetType instanceof ShortBuiltinType ||
                targetType instanceof IntBuiltinType || targetType instanceof LongBuiltinType) {
            return emitAndReturn(cctx, "zext i1 " + valueRegister + " to " + targetType.getLLVMName());
        }
        if (targetType instanceof FloatBuiltinType || targetType instanceof DoubleBuiltinType) {
            return emitAndReturn(cctx, "uitofp i1 " + valueRegister + " to " + targetType.getLLVMName());
        }

        return incompatible(fileName, line, BuiltinTypes.BOOL.getType(), targetType);
    }

    private static String fromChar(final String fileName,
                                   final int line,
                                   final String valueRegister,
                                   final TypeRef targetType,
                                   final CompilationContext cctx) {
        if (targetType instanceof CharBuiltinType || targetType instanceof ByteBuiltinType) {
            return valueRegister;
        }

        if (targetType instanceof ShortBuiltinType || targetType instanceof IntBuiltinType || targetType instanceof LongBuiltinType) {
            return emitAndReturn(cctx, "zext i8 " + valueRegister + " to " + targetType.getLLVMName());
        }
        if (targetType instanceof FloatBuiltinType || targetType instanceof DoubleBuiltinType) {
            return emitAndReturn(cctx, "uitofp i8 " + valueRegister + " to " + targetType.getLLVMName());
        }

        return incompatible(fileName, line, BuiltinTypes.CHAR.getType(), targetType);
    }

    private static String fromAnyPointer(final String fileName,
                                         final int line,
                                         final String valueRegister,
                                         final TypeRef targetType,
                                         final CompilationContext cctx) {
        if (targetType instanceof AnyPointerType) return valueRegister;

        if (targetType instanceof ArrayType arrayType) {
            final TypeRef elementType = arrayType.getInner();
            final PointerType pointerToElement = new PointerType(elementType);
            final String result = newRegister(cctx);
            cctx.emit(result + " = bitcast i8* " + valueRegister + " to " + pointerToElement.getLLVMName());
            return result;
        }

        if (targetType instanceof StrBuiltinType) {
            return valueRegister;
        }

        if (targetType instanceof PointerType) {
            final String result = newRegister(cctx);
            cctx.emit(result + " = bitcast i8* " + valueRegister + " to " + targetType.getLLVMName());
            return result;
        }

        return incompatible(fileName, line, BuiltinTypes.ANYPTR.getType(), targetType);
    }

    private static String fromPointer(final String fileName,
                                      final int line,
                                      final TypeRef sourceInner,
                                      final String valueRegister,
                                      final TypeRef targetType,
                                      final CompilationContext cctx) {
        final PointerType sourcePointer = new PointerType(sourceInner);

        if (targetType instanceof PointerType targetPointer) {
            if (sourcePointer.getLLVMName().equals(targetPointer.getLLVMName())) {
                return valueRegister;
            }

            final String result = newRegister(cctx);
            cctx.emit(result + " = bitcast " + sourcePointer.getLLVMName() + " " + valueRegister + " to " + targetPointer.getLLVMName());
            return result;
        }

        if (targetType instanceof LongBuiltinType) {
            return emitAndReturn(cctx, "ptrtoint " + sourcePointer.getLLVMName() + " " + valueRegister + " to i64");
        }

        if (targetType instanceof AnyPointerType || (targetType instanceof StrBuiltinType && sourceInner instanceof CharBuiltinType)) {
            final String result = newRegister(cctx);
            cctx.emit(result + " = bitcast " + sourcePointer.getLLVMName() + " " + valueRegister + " to " + targetType.getLLVMName());
            return result;
        }

        return incompatible(fileName, line, sourcePointer, targetType);
    }

    private static String fromArray(final String fileName,
                                    final int line,
                                    final ArrayType sourceArray,
                                    final String valueRegister,
                                    final TypeRef targetType,
                                    final CompilationContext cctx) {
        if (!(targetType instanceof AnyPointerType || targetType instanceof PointerType)) {
            return incompatible(fileName, line, sourceArray, targetType);
        }

        final String result = newRegister(cctx);
        final PointerType pointerToElement = new PointerType(sourceArray.getInner());
        cctx.emit(result + " = bitcast " + pointerToElement.getLLVMName() + " " + valueRegister + " to " + targetType.getLLVMName());
        return result;
    }

    private static String fromStr(final String fileName,
                                  final int line,
                                  final String valueRegister,
                                  final TypeRef targetType,
                                  final CompilationContext cctx) {
        if (targetType instanceof StrBuiltinType) return valueRegister;

        if (targetType instanceof AnyPointerType || targetType instanceof PointerType) {
            final String result = newRegister(cctx);
            cctx.emit(result + " = bitcast " + BuiltinTypes.STR.getType().getLLVMName() + " " + valueRegister + " to " + targetType.getLLVMName());
            return result;
        }

        if (targetType instanceof LongBuiltinType) {
            return emitAndReturn(cctx, "ptrtoint " + BuiltinTypes.STR.getType().getLLVMName() + " " + valueRegister + " to i64");
        }

        return incompatible(fileName, line, BuiltinTypes.STR.getType(), targetType);
    }

    private static String fromUnion(final String fileName,
                                    final int line,
                                    final UnionType sourceUnion,
                                    final String valueRegister,
                                    final TypeRef targetType,
                                    final CompilationContext cctx) {
        if (!(targetType instanceof StructType structTarget) || !sourceUnion.contains(targetType)) {
            return incompatible(fileName, line, sourceUnion, targetType);
        }

        final String unionPtr = newRegister(cctx);
        cctx.emit(unionPtr + " = alloca " + sourceUnion.getLLVMName());
        cctx.emit("store " + sourceUnion.getLLVMName() + " " + valueRegister + ", " + sourceUnion.getLLVMName() + "* " + unionPtr);

        final String payloadPtr = newRegister(cctx);
        cctx.emit(payloadPtr + " = getelementptr inbounds " + sourceUnion.getLLVMName() + ", " + sourceUnion.getLLVMName() + "* " + unionPtr + ", i32 0, i32 1");

        final String structPtr = newRegister(cctx);
        cctx.emit(structPtr + " = bitcast [" + sourceUnion.payloadSize() + " x i8]* " + payloadPtr + " to " + structTarget.getLLVMName() + "*");

        final String result = newRegister(cctx);
        cctx.emit(result + " = load " + structTarget.getLLVMName() + ", " + structTarget.getLLVMName() + "* " + structPtr);
        return result;
    }

    private static String fromStruct(final String fileName,
                                     final int line,
                                     final StructType sourceStruct,
                                     final String valueRegister,
                                     final TypeRef targetType,
                                     final CompilationContext cctx) {
        if (!(targetType instanceof UnionType unionTarget)) {
            return incompatible(fileName, line, sourceStruct, targetType);
        }

        final int tag = unionTarget.variants().indexOf(sourceStruct);
        if (tag < 0) {
            return incompatible(fileName, line, sourceStruct, targetType);
        }

        final String unionPtr = newRegister(cctx);
        cctx.emit(unionPtr + " = alloca " + unionTarget.getLLVMName());

        final String tagPtr = newRegister(cctx);
        cctx.emit(tagPtr + " = getelementptr inbounds " + unionTarget.getLLVMName() + ", " + unionTarget.getLLVMName() + "* " + unionPtr + ", i32 0, i32 0");
        cctx.emit("store i32 " + tag + ", i32* " + tagPtr);

        final String payloadPtr = newRegister(cctx);
        cctx.emit(payloadPtr + " = getelementptr inbounds " + unionTarget.getLLVMName() + ", " + unionTarget.getLLVMName() + "* " + unionPtr + ", i32 0, i32 1");

        final String structPtr = newRegister(cctx);
        cctx.emit(structPtr + " = bitcast [" + (unionTarget.getSize() - 4) + " x i8]* " + payloadPtr + " to " + sourceStruct.getLLVMName() + "*");

        cctx.emit("store " + sourceStruct.getLLVMName() + " " + valueRegister + ", " + sourceStruct.getLLVMName() + "* " + structPtr);

        final String result = newRegister(cctx);
        cctx.emit(result + " = load " + unionTarget.getLLVMName() + ", " + unionTarget.getLLVMName() + "* " + unionPtr);
        return result;
    }
}
