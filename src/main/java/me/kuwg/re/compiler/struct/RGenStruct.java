package me.kuwg.re.compiler.struct;

import me.kuwg.re.compiler.variable.RStructField;
import me.kuwg.re.type.TypeRef;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RGenStruct extends RStruct {
    private final Map<List<TypeRef>, RStruct> instantiations = new HashMap<>();

    public RGenStruct(final TypeRef type, final List<RStructField> fields) {
        super(false, type, fields);
    }

    public RStruct getInstantiation(List<TypeRef> bindings) {
        return instantiations.get(bindings);
    }

    public void addInstantiation(List<TypeRef> bindings, RStruct struct) {
        instantiations.put(bindings, struct);
    }
}
