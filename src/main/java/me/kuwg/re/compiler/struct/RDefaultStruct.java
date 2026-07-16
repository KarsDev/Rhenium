package me.kuwg.re.compiler.struct;

import me.kuwg.re.compiler.function.RFunction;
import me.kuwg.re.compiler.variable.RStructField;
import me.kuwg.re.type.TypeRef;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class RDefaultStruct {
    protected final String fileName;
    protected final List<RFunction> constructors = new ArrayList<>();
    private final boolean builtin;
    protected final TypeRef type;
    protected final List<String> inherited;
    protected final List<RStructField> fields;
    protected final List<RFunction> functions = new ArrayList<>();

    private RFunction destructor = null;

    public RDefaultStruct(final String fileName, final boolean builtin, final List<String> inherited, final TypeRef type, final List<RStructField> fields) {
        this.fileName = fileName;
        this.builtin = builtin;
        this.inherited = inherited;
        this.type = type;
        this.fields = fields;
    }

    public boolean builtin() {
        return builtin;
    }

    public List<String> inherited() {
        return inherited;
    }

    public TypeRef type() {
        return type;
    }

    public List<RFunction> constructors() {
        return constructors;
    }

    public List<RStructField> fields() {
        return fields;
    }

    public List<RFunction> functions() {
        return functions;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (RDefaultStruct) obj;
        return Objects.equals(this.type, that.type) && Objects.equals(this.fields, that.fields) && Objects.equals(this.functions, that.functions);
    }

    public void setDestructor(final RFunction compiled) {
        this.destructor = compiled;
    }

    public RFunction getDestructor() {
        return destructor;
    }
}
