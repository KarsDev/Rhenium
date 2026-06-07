package me.kuwg.re.ast.nodes.struct;

import me.kuwg.re.ast.ASTNode;
import me.kuwg.re.ast.types.global.GlobalNode;
import me.kuwg.re.compiler.CompilationContext;
import me.kuwg.re.compiler.struct.RGenStruct;
import me.kuwg.re.compiler.variable.RStructField;
import me.kuwg.re.error.errors.struct.RStructAlreadyExistsError;
import me.kuwg.re.error.errors.struct.RStructGenFieldError;
import me.kuwg.re.type.TypeRef;
import me.kuwg.re.type.struct.AppliedGenStructType;
import me.kuwg.re.type.struct.StructType;

import java.util.List;
import java.util.Map;

public class StructDeclarationNode extends ASTNode implements GlobalNode {
    private final boolean builtin;
    private final String name;
    private final List<String> inherited;
    private final StructType type;
    private final List<RStructField> fields;

    public StructDeclarationNode(final int line, final boolean builtin, final String name, final List<String> inherited,
                                 final StructType type, final List<RStructField> fields) {
        super(line);
        this.builtin = builtin;
        this.name = name;
        this.inherited = inherited;
        this.type = type;
        this.fields = fields;
    }

    @Override
    public void replaceGenerics(final Map<String, TypeRef> generics, final CompilationContext cctx) {
        for (int i = 0; i < fields.size(); i++) {
            RStructField field = fields.get(i);
            TypeRef replaced = replaceGenericType(field.type(), generics, cctx);
            fields.set(i, new RStructField(field.name(), replaced));
        }
    }

    @Override
    public void compile(final CompilationContext cctx) {
        if (cctx.getStruct(name) != null) {
            new RStructAlreadyExistsError(name, line).raise();
            return;
        }

        cctx.addStruct(builtin, name, inherited, type, fields);

        String mangledName = cctx.getStruct(name).type().getMangledName();
        StringBuilder sb = new StringBuilder();
        sb.append("; Struct declaration\n");
        sb.append("%struct.").append(mangledName).append(" = type { ");

        for (int i = 0; i < fields.size(); i++) {
            TypeRef fieldType = resolveFieldType(fields.get(i).type(), cctx);

            if (fieldType instanceof AppliedGenStructType applied) {
                throw new IllegalStateException("Unresolved applied generic struct field: " + applied.getName() + " in struct " + name);
            }

            sb.append(fieldType.getLLVMName());
            if (i + 1 < fields.size()) sb.append(", ");
        }

        sb.append(" }");
        cctx.declare(sb.toString());
    }

    private TypeRef resolveFieldType(final TypeRef original, final CompilationContext cctx) {
        if (!(original instanceof AppliedGenStructType applied)) {
            return original;
        }

        var existing = cctx.getStruct(applied.getMangledName());
        if (existing != null) {
            return existing.type();
        }

        var baseStruct = cctx.getStruct(applied.base().getName());
        if (baseStruct instanceof RGenStruct genStruct) {
            return genStruct.instantiate(applied.args(), cctx).type();
        }

        return new RStructGenFieldError("Cannot resolve applied generic struct type '" + applied.getName() +
                "'; " + "no generic struct template named '" + applied.base().getName() + "' was found.", line).raise();
    }

    @Override
    public void write(final StringBuilder sb, final String indent) {
        sb.append(indent).append("Struct Declaration: ").append(NEWLINE);
        sb.append(indent).append(TAB).append("Name: ").append(name).append(NEWLINE);
        sb.append(indent).append(TAB).append("Fields: ").append(NEWLINE);
        for (final RStructField field : fields) {
            sb.append(indent).append(TAB).append(TAB).append("Name: ").append(field.name()).append(", Type: ").append(field.type().getName()).append(NEWLINE);
        }
    }

    @Override
    public StructDeclarationNode clone() {
        return this;
    }
}