package me.kuwg.re.ast.nodes.function.call;

import me.kuwg.re.ast.nodes.cast.CastNode;
import me.kuwg.re.ast.types.value.ValueNode;
import me.kuwg.re.compiler.CompilationContext;
import me.kuwg.re.compiler.function.RFunction;
import me.kuwg.re.compiler.function.RGenFunction;
import me.kuwg.re.compiler.generic.TypeParameter;
import me.kuwg.re.compiler.struct.RDefaultStruct;
import me.kuwg.re.error.errors.function.RFunctionGenericsError;
import me.kuwg.re.error.errors.function.RFunctionIsVoidError;
import me.kuwg.re.error.errors.function.RFunctionNotFoundError;
import me.kuwg.re.type.TypeRef;
import me.kuwg.re.type.builtin.NoneBuiltinType;
import me.kuwg.re.type.generic.GenericType;
import me.kuwg.re.type.iterable.arr.ArrayType;
import me.kuwg.re.type.ptr.PointerType;

import java.util.*;

public abstract class FunCall extends ValueNode {
    final String name;
    final List<ValueNode> parameters;

    protected FunCall(final String fileName, final int line, final String name, final List<ValueNode> parameters) {
        super(fileName, line);
        this.name = name;
        this.parameters = parameters;
    }

    void validateGenericUsage(RGenFunction fn) {
        Set<TypeParameter> allowed = new HashSet<>(fn.typeParameters());
        for (var p : fn.parameters()) {
            if (p.type() instanceof GenericType g && allowed.stream().noneMatch(a -> a.name().equals(g.name()))) {
                new RFunctionGenericsError("Unknown type parameter: " + g.name(), fileName, line).raise();
            }

        }
        if (fn.returnType() instanceof GenericType g && allowed.stream().noneMatch(a -> a.name().equals(g.name()))) {
            new RFunctionGenericsError("Unknown type parameter: " + g.name(), fileName, line).raise();
        }
    }

    TypeRef substituteConcrete(TypeRef type, Map<String, TypeRef> map) {
        if (type instanceof GenericType gen) {
            TypeRef resolved = map.get(gen.name());
            if (resolved == null) {
                return new RFunctionGenericsError("Unresolved generic type: " + gen.name(), fileName, line).raise();
            }
            return resolved;
        } else if (type instanceof PointerType ptr) {
            return new PointerType(substituteConcrete(ptr.inner(), map));
        } else if (type instanceof ArrayType arr) {
            return new ArrayType(arr.size(), substituteConcrete(arr.inner(), map));
        }
        return type;
    }

    boolean containsGeneric(TypeRef type) {
        if (type instanceof GenericType) return true;
        if (type instanceof ArrayType arr) return containsGeneric(arr.inner());
        if (type instanceof PointerType ptr) return containsGeneric(ptr.inner());
        return false;
    }

    String emitCall(CompilationContext cctx, RFunction fn, List<String> argRegs, List<TypeRef> callTypes, boolean getting) {
        if (fn.returnType() instanceof NoneBuiltinType && getting) {
            throwVoid(fn, callTypes);
        }

        if (argRegs == null) {
            argRegs = new ArrayList<>();
            callTypes = new ArrayList<>();
            for (ValueNode param : parameters) {
                argRegs.add(param.compileAndGet(cctx));
                callTypes.add(param.getType());
            }
        }

        for (int i = 0; i < parameters.size(); i++) {
            TypeRef expected = fn.parameters().get(i).type();
            TypeRef actual = callTypes.get(i);

            if (containsGeneric(expected)) {
                return new RFunctionGenericsError("Generic type leaked into concrete call", fileName, line).raise();
            }

            if (!actual.equals(expected)) {
                CastNode cast = new CastNode(fileName, line, expected, parameters.get(i));
                argRegs.set(i, cast.compileAndGet(cctx));
                callTypes.set(i, expected);
            }
        }

        StringBuilder sb = new StringBuilder();
        String result = null;

        if (!(fn.returnType() instanceof NoneBuiltinType)) {
            result = cctx.nextRegister();
            sb.append(result).append(" = ");
        }

        sb.append("call ").append(fn.returnType().getLLVMName()).append(" @").append(fn.llvmName).append("(");

        for (int i = 0; i < argRegs.size(); i++) {
            sb.append(evalType(callTypes.get(i), cctx, fileName, line).getLLVMName()).append(" ").append(argRegs.get(i));
            if (i < argRegs.size() - 1) sb.append(", ");
        }

        sb.append(")");
        cctx.emit(sb.toString());

        setType(fn.returnType());
        return fn.returnType() instanceof NoneBuiltinType ? "%void_" + cctx.nextRegister() : result;
    }

    <T> T throwNotFound(List<TypeRef> types) {
        StringBuilder sb = new StringBuilder("(");
        for (int i = 0; i < types.size(); i++) {
            sb.append(types.get(i).getName());
            if (i < types.size() - 1) sb.append(", ");
        }
        sb.append(")");
        return new RFunctionNotFoundError(name, sb.toString(), fileName, line).raise();
    }

    void throwVoid(RFunction fn, List<TypeRef> types) {
        StringBuilder sb = new StringBuilder("(");
        for (int i = 0; i < types.size(); i++) {
            sb.append(types.get(i).getName());
            if (i < types.size() - 1) sb.append(", ");
        }
        sb.append(")");
        new RFunctionIsVoidError(fn.name(), sb.toString(), fileName, line).raise();
    }

    void validateGenericConstraints(CompilationContext cctx, RGenFunction fn, Map<String, TypeRef> bindings) {
        for (var tp : fn.typeParameters()) {
            if (tp.inherited() == null) {
                continue;
            }

            TypeRef actual = bindings.get(tp.name());

            if (actual == null) {
                continue;
            }

            if (!satisfiesConstraint(cctx, actual.getName(), tp.inherited())) {
                new RFunctionGenericsError("Type '" + actual.getName() + "' does not satisfy constraint '" + tp.inherited() + "' for generic '" + tp.name() + "'", fileName, line).raise();
            }
        }
    }

    boolean satisfiesConstraint(CompilationContext cctx, String actual, String required) {
        if (actual.equals(required)) {
            return true;
        }

        RDefaultStruct struct = cctx.getStruct(actual);

        if (struct == null) {
            return false;
        }

        return inherits(cctx, struct, required);
    }

    boolean inherits(CompilationContext cctx, RDefaultStruct struct, String required) {
        for (String parent : struct.inherited()) {

            if (parent.equals(required)) {
                return true;
            }

            RDefaultStruct inheritedStruct = cctx.getStruct(parent);

            if (inheritedStruct != null && inherits(cctx, inheritedStruct, required)) {
                return true;
            }
        }

        return false;
    }
}
