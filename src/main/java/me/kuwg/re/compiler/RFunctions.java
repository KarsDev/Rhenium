package me.kuwg.re.compiler;

import me.kuwg.re.compiler.function.RFunction;
import me.kuwg.re.type.TypeRef;
import me.kuwg.re.type.generic.GenericType;

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

    RFunction get(String name, List<TypeRef> parameters) {
        RFunction compatible = null;

        for (final RFunction fn : functions) {
            if (!fn.name().equals(name) || fn.parameters().size() != parameters.size()) continue;

            boolean exact = true;
            boolean isCompatible = true;

            var bindings = new HashMap<String, TypeRef>();

            for (int i = 0; i < parameters.size(); i++) {
                var fnParamType = fn.parameters().get(i).type();
                var callParamType = parameters.get(i);

                if (fnParamType instanceof GenericType gen) {
                    if (!matchGeneric(gen, callParamType, bindings)) {
                        isCompatible = false;
                        break;
                    }
                    exact = false;
                    continue;
                }

                if (!fnParamType.equals(callParamType)) {
                    exact = false;
                }

                if (!fnParamType.isCompatibleWith(callParamType)) {
                    isCompatible = false;
                    break;
                }
            }

            if (exact) return fn;
            if (isCompatible && compatible == null) compatible = fn;
        }

        return compatible;
    }

    private boolean matchGeneric(GenericType generic, TypeRef concrete, Map<String, TypeRef> bindings) {
        TypeRef bound = bindings.get(generic.name());
        if (bound == null) {
            bindings.put(generic.name(), concrete);
            return true;
        }
        return bound.equals(concrete);
    }

    List<RFunction> get(String name) {
        return functions.stream().filter(fn -> fn.name().equals(name)).toList();
    }
}
