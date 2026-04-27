package me.kuwg.re.compiler.struct;

import me.kuwg.re.compiler.CompilationContext;
import me.kuwg.re.compiler.variable.RStructField;
import me.kuwg.re.type.TypeRef;
import me.kuwg.re.type.generic.GenericType;
import me.kuwg.re.type.struct.GenStructType;
import me.kuwg.re.type.struct.StructType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RGenStruct extends RDefaultStruct {
    private final Map<List<TypeRef>, RStruct> cache = new HashMap<>();

    public RGenStruct(TypeRef type, List<RStructField> fields) {
       super(false, type, fields);
    }

    @Override
    public GenStructType type() {
        return (GenStructType) super.type();
    }

    public RStruct instantiate(List<TypeRef> types, CompilationContext cctx) {
        if (cache.containsKey(types)) {
            return cache.get(types);
        }

        Map<String, TypeRef> mapping = new HashMap<>();

        for (int i = 0; i < type().genericTypes().size(); i++) {
            mapping.put(type().genericTypes().get(i), types.get(i));
        }

        List<RStructField> newFields = new ArrayList<>();

        for (RStructField field : fields) {
            TypeRef replaced = replace(field.type(), mapping);
            newFields.add(new RStructField(field.name(), replaced));
        }

        String mangledName = mangleName(types);

        TypeRef newType = new StructType(
                mangledName,
                newFields.stream().map(RStructField::type).toList()
        );

        RStruct specialized = new RStruct(false, newType, newFields);

        cache.put(types, specialized);
        cctx.addStruct(false, mangledName, newType, newFields);

        declareStructIfNeeded(cctx, specialized);

        return specialized;
    }

    private void declareStructIfNeeded(CompilationContext cctx, RStruct struct) {
        String name = struct.type().getLLVMName();

        if (cctx.isStructDeclared(name)) return;

        cctx.markStructDeclared(name);

        StringBuilder sb = new StringBuilder();
        sb.append(name).append(" = type { ");

        List<String> fieldTypes = struct.fields().stream()
                .map(f -> f.type().getLLVMName())
                .toList();

        sb.append(String.join(", ", fieldTypes));
        sb.append(" }");

        cctx.addIR(sb.toString());
    }

    private TypeRef replace(TypeRef original, Map<String, TypeRef> mapping) {
        if (original instanceof GenericType gen) {
            TypeRef resolved = mapping.get(gen.name());

            if (resolved == null) {
                throw new RuntimeException("Unmapped generic type: " + gen.name());
            }

            return resolved;
        }

        if (original instanceof GenStructType genType) {
            List<TypeRef> replacedFields = new ArrayList<>();

            for (TypeRef fieldType : genType.fieldTypes()) {
                replacedFields.add(replace(fieldType, mapping));
            }

            return new GenStructType(
                    genType.genericTypes(),
                    genType.getName(),
                    replacedFields
            );
        }

        return original;
    }

    private String mangleName(List<TypeRef> types) {
        StringBuilder sb = new StringBuilder();
        sb.append(type().getName());
        sb.append("$");

        for (TypeRef t : types) {
            sb.append(encodeType(t));
        }

        return sb.toString();
    }

    private String encodeType(TypeRef type) {
        String name = sanitize(type.getName());

        String base = name.length() + name;

        if (type instanceof GenStructType gen) {
            StringBuilder sb = new StringBuilder();
            sb.append("G");
            sb.append(base);

            for (TypeRef inner : gen.fieldTypes()) {
                sb.append(encodeType(inner));
            }

            return sb.toString();
        }

        return base;
    }

    private String sanitize(String s) {
        return s.replaceAll("[^A-Za-z0-9_]", "_");
    }
}
