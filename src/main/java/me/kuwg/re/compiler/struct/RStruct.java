package me.kuwg.re.compiler.struct;

import me.kuwg.re.compiler.function.RFunction;
import me.kuwg.re.compiler.variable.RStructField;
import me.kuwg.re.type.TypeRef;
import me.kuwg.re.type.struct.GenStructType;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public abstract class RStruct {
    private final boolean builtin;
    private final TypeRef type;
    private final List<RStructField> fields;
    private final List<RFunction> functions = new ArrayList<>();

    public RStruct(boolean builtin, TypeRef type, List<RStructField> fields) {
        this.builtin = builtin;
        this.type = type;
        this.fields = fields;
    }

    public boolean builtin() {
        return builtin;
    }

    public TypeRef type() {
        return type;
    }

    public List<RStructField> fields() {
        return fields;
    }

    public List<RFunction> functions() {
        return functions;
    }

    public boolean isGeneric() {
        return type instanceof GenStructType;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (RStruct) obj;
        return this.builtin == that.builtin &&
                Objects.equals(this.type, that.type) &&
                Objects.equals(this.fields, that.fields) &&
                Objects.equals(this.functions, that.functions);
    }

    @Override
    public int hashCode() {
        return Objects.hash(builtin, type, fields, functions);
    }

    @Override
    public String toString() {
        return "RStruct[" +
                "builtin=" + builtin + ", " +
                "type=" + type + ", " +
                "fields=" + fields + ", " +
                "functions=" + functions + ']';
    }

}
