package me.kuwg.re.ast.nodes.struct.gen;

import me.kuwg.re.ast.ASTNode;
import me.kuwg.re.compiler.CompilationContext;
import me.kuwg.re.compiler.variable.RStructField;
import me.kuwg.re.error.errors.struct.RStructAlreadyExistsError;
import me.kuwg.re.type.TypeRef;
import me.kuwg.re.type.struct.GenStructType;

import java.util.List;
import java.util.Map;

public class GenStructDeclarationNode extends ASTNode {
    private final String name;
    private final List<String> inherited;
    private final GenStructType type;
    private final List<RStructField> fields;

    public GenStructDeclarationNode(final String fileName, final int line, final String name, final List<String> inherited, final GenStructType type, final List<RStructField> fields) {
        super(fileName, line);
        this.name = name;
        this.inherited = inherited;
        this.type = type;
        this.fields = fields;
    }

    @Override
    public void replaceGenerics(final Map<String, TypeRef> generics, final CompilationContext cctx) {
    }

    @Override
    public void compile(final CompilationContext cctx) {
        cctx.declare("; Generic Struct declaration");

        if (cctx.getStruct(name) != null) {
            new RStructAlreadyExistsError(name, fileName, line).raise();
            return;
        }

        cctx.addStruct(false, name, inherited, type, fields);
    }

    @Override
    public void write(final StringBuilder sb, final String indent) {
        sb.append(indent).append("Generic Struct Declaration: ").append(NEWLINE);
        sb.append(indent).append(TAB).append("Name: ").append(name).append(NEWLINE);
        sb.append(indent).append(TAB).append("Types: ").append(NEWLINE);
        type.genericTypes().forEach(t -> sb.append(indent).append(TAB).append(TAB).append("- ").append(t).append(NEWLINE));
        sb.append(indent).append(TAB).append("Fields: ").append(NEWLINE);
        for (final RStructField field : fields) {
            sb.append(indent).append(TAB).append(TAB).append("Name: ").append(field.name()).append(", Type: ")
                    .append(field.type().getName()).append(NEWLINE);
        }
    }

    @Override
    public GenStructDeclarationNode clone() {
        return this;
    }
}
