package me.kuwg.re.ast.nodes.struct.gen;

import me.kuwg.re.ast.types.value.ValueNode;
import me.kuwg.re.compiler.CompilationContext;
import me.kuwg.re.compiler.struct.RDefaultStruct;
import me.kuwg.re.compiler.struct.RGenStruct;
import me.kuwg.re.compiler.struct.RStruct;
import me.kuwg.re.compiler.struct.StructCompiler;
import me.kuwg.re.compiler.variable.RParamValue;
import me.kuwg.re.error.errors.struct.RGenStructInitError;
import me.kuwg.re.error.errors.struct.RStructUndefinedError;
import me.kuwg.re.type.TypeRef;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

public class GenStructInitNode extends ValueNode {
    private final String name;
    private final List<TypeRef> genericTypes;
    private final List<RParamValue> values;

    public GenStructInitNode(final String fileName, final int line, final String name, final List<TypeRef> genericTypes, final List<RParamValue> values) {
        super(fileName, line);
        this.name = name;
        this.genericTypes = genericTypes;
        this.values = values;
    }

    @Override
    public void replaceGenerics(final Map<String, TypeRef> generics, final CompilationContext cctx) {
        genericTypes.replaceAll(current -> replaceGenericType(current, generics, cctx));
        values.forEach(v -> v.value().replaceGenerics(generics, cctx));
    }

    @Override
    public String compileAndGet(final CompilationContext cctx) {
        cctx.emit("; Generic struct initialization");

        RDefaultStruct struct = cctx.getStruct(name);

        if (struct == null) {
            return new RStructUndefinedError(name, fileName, line).raise();
        }

        if (struct instanceof RStruct) {
            return new RGenStructInitError("Struct is not generic: " + name, fileName, line).raise();
        }

        RGenStruct genStruct = (RGenStruct) struct;

        if (genStruct.type().getGenericTypes().size() != genericTypes.size()) {
            return new RGenStructInitError("Expected " + genStruct.type().getGenericTypes().size() + " generic types but got " + genericTypes.size(), fileName, line).raise();
        }

        Map<String, TypeRef> bindings = new HashMap<>();

        var declared = genStruct.type().getGenericTypes();

        for (int i = 0; i < declared.size(); i++) {
            bindings.put(declared.get(i).name(), genericTypes.get(i));
        }

        replaceGenerics(bindings, cctx);

        RStruct specialized = genStruct.instantiate(genericTypes, cctx, line);

        return StructCompiler.compile(fileName, line, cctx, specialized, values, this);
    }

    @Override
    public void compile(final CompilationContext cctx) {
        compileAndGet(cctx);
    }

    @Override
    public void write(final StringBuilder sb, final String indent) {
        sb.append(indent).append("Generic Struct Init: ").append(NEWLINE);
        sb.append(indent).append(TAB).append("Name: ").append(name).append(NEWLINE);
        sb.append(indent).append(TAB).append("Types: ").append(NEWLINE);
        genericTypes.forEach(t -> sb.append(indent).append(TAB).append(TAB).append("- ").append(t.getName()).append(NEWLINE));
        sb.append(indent).append(TAB).append("Values: ").append(NEWLINE);
        values.forEach(v -> v.value().write(sb, indent + TAB + TAB));
    }

    @Override
    public GenStructInitNode clone() {
        List<RParamValue> valuesCloned = new ArrayList<>();
        IntStream.range(0, values.size()).forEach(i -> valuesCloned.add(i, values.get(i).clone()));
        return new GenStructInitNode(fileName, line, name, genericTypes, valuesCloned);
    }
}
