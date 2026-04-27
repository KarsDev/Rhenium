package me.kuwg.re.compiler.struct;

import me.kuwg.re.compiler.variable.RStructField;
import me.kuwg.re.type.TypeRef;

import java.util.List;

public class RStruct extends RDefaultStruct {
    public RStruct(final boolean builtin, final TypeRef type, final List<RStructField> fields) {
        super(builtin, type, fields);
    }
}
