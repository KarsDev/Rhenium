package me.kuwg.re.compiler;

import me.kuwg.re.compiler.function.RFunction;
import me.kuwg.re.type.TypeRef;
import me.kuwg.re.type.generic.GenericType;
import me.kuwg.re.type.iterable.arr.ArrayType;
import me.kuwg.re.type.ptr.PointerType;
import me.kuwg.re.type.struct.AppliedGenStructType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class RFunctions {
    private final List<RFunction> functions;

    RFunctions() {
        functions = new ArrayList<>();
    }

    void add(RFunction function) {
        functions.add(function);
    }

    void writeAll() {
        System.out.println(functions);
    }

    RFunction get(String name, List<TypeRef> parameters) {
        RFunction compatible = null;

        for (final RFunction fn : functions) {
            if (!fn.name().equals(name) || fn.parameters().size() != parameters.size()) {
                continue;
            }

            boolean exact = true;
            boolean isCompatible = true;

            Map<String, TypeRef> bindings = new HashMap<>();

            for (int i = 0; i < parameters.size(); i++) {
                TypeRef fnParamType = fn.parameters().get(i).type();
                TypeRef callParamType = parameters.get(i);

                MatchResult result = matchTypes(fnParamType, callParamType, bindings);

                if (!result.matches) {
                    isCompatible = false;
                    break;
                }

                if (!result.exact) {
                    exact = false;
                }
            }

            if (exact) return fn;
            if (isCompatible && compatible == null) compatible = fn;
        }

        return compatible;
    }

    private MatchResult matchTypes(TypeRef param, TypeRef arg, Map<String, TypeRef> bindings) {
        if (param instanceof GenericType gen) {
            TypeRef bound = bindings.get(gen.name());

            if (bound == null) {
                bindings.put(gen.name(), arg);
                return new MatchResult(true, false);
            }

            return new MatchResult(bound.equals(arg), false);
        }

        if (param instanceof AppliedGenStructType pStruct &&
                arg   instanceof AppliedGenStructType aStruct) {

            if (!pStruct.base().getName().equals(aStruct.base().getName())) {
                return new MatchResult(false, false);
            }

            if (pStruct.args().size() != aStruct.args().size()) {
                return new MatchResult(false, false);
            }

            boolean exact = true;

            for (int i = 0; i < pStruct.args().size(); i++) {
                MatchResult r = matchTypes(
                        pStruct.args().get(i),
                        aStruct.args().get(i),
                        bindings
                );

                if (!r.matches) {
                    return new MatchResult(false, false);
                }

                if (!r.exact) {
                    exact = false;
                }
            }

            return new MatchResult(true, exact);
        }

        if (param instanceof ArrayType pArr &&
                arg   instanceof ArrayType aArr) {

            return matchTypes(pArr.inner(), aArr.inner(), bindings);
        }

        if (param instanceof PointerType pPtr &&
                arg   instanceof PointerType aPtr) {

            return matchTypes(pPtr.inner(), aPtr.inner(), bindings);
        }

        boolean eq = param.equals(arg);
        return new MatchResult(eq, eq);
    }

    List<RFunction> get(String name) {
        return functions.stream().filter(fn -> fn.name().equals(name)).toList();
    }

    private static class MatchResult {
        private final boolean matches;
        private final boolean exact;

        MatchResult(boolean matches, boolean exact) {
            this.matches = matches;
            this.exact = exact;
        }
    }
}