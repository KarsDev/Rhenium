package me.kuwg.re.ast.nodes.function.call;

import me.kuwg.re.ast.nodes.cast.CastNode;
import me.kuwg.re.ast.types.value.ValueNode;
import me.kuwg.re.compiler.CompilationContext;
import me.kuwg.re.compiler.function.RFunction;
import me.kuwg.re.compiler.function.RGenFunction;
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

    protected FunCall(final int line, final String name, final List<ValueNode> parameters) {
        super(line);
        this.name = name;
        this.parameters = parameters;
    }

    void validateGenericUsage(RGenFunction fn) {
        Set<String> allowed = new HashSet<>(fn.typeParameters());
        for (var p : fn.parameters()) {
            if (p.type() instanceof GenericType g && !allowed.contains(g.name())) {
                new RFunctionGenericsError("Unknown type parameter: " + g.name(), line).raise();
            }
        }
        if (fn.returnType() instanceof GenericType g && !allowed.contains(g.name())) {
            new RFunctionGenericsError("Unknown type parameter: " + g.name(), line).raise();
        }
    }

    TypeRef substituteConcrete(TypeRef type, Map<String, TypeRef> map) {
        if (type instanceof GenericType gen) {
            TypeRef resolved = map.get(gen.name());
            if (resolved == null) {
                return new RFunctionGenericsError("Unresolved generic type: " + gen.name(), line).raise();
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
                return new RFunctionGenericsError("Generic type leaked into concrete call", line).raise();
            }

            if (!actual.equals(expected)) {
                CastNode cast = new CastNode(line, expected, parameters.get(i));
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
            sb.append(evalType(callTypes.get(i), cctx).getLLVMName()).append(" ").append(argRegs.get(i));
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
        return new RFunctionNotFoundError(name, sb.toString(), line).raise();
    }

    void throwVoid(RFunction fn, List<TypeRef> types) {
        StringBuilder sb = new StringBuilder("(");
        for (int i = 0; i < types.size(); i++) {
            sb.append(types.get(i).getName());
            if (i < types.size() - 1) sb.append(", ");
        }
        sb.append(")");
        new RFunctionIsVoidError(fn.name(), sb.toString(), line).raise();
    }
}
