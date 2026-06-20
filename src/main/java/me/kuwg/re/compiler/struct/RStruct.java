package me.kuwg.re.compiler.struct;

import me.kuwg.re.compiler.variable.RStructField;
import me.kuwg.re.type.TypeRef;

import java.util.List;

public final class RStruct extends RDefaultStruct {
    public RStruct(final String fileName, final boolean builtin, final List<String> inherited, final TypeRef type, final List<RStructField> fields) {
        super(fileName, builtin, inherited, type, fields);
    }
}
