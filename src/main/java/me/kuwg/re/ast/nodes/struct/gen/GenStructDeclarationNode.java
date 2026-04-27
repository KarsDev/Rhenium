package me.kuwg.re.ast.nodes.struct.gen;

import me.kuwg.re.ast.ASTNode;
import me.kuwg.re.compiler.CompilationContext;
import me.kuwg.re.compiler.variable.RStructField;
import me.kuwg.re.error.errors.struct.RStructAlreadyExistsError;
import me.kuwg.re.type.struct.GenStructType;

import java.util.List;

public class GenStructDeclarationNode extends ASTNode {
    private final String name;
    private final GenStructType type;
    private final List<RStructField> fields;

    public GenStructDeclarationNode(final int line, final String name, final GenStructType type, final List<RStructField> fields) {
        super(line);
        this.name = name;
        this.type = type;
        this.fields = fields;
    }

    @Override
    public void compile(final CompilationContext cctx) {
        if (cctx.getStruct(name) != null) {
            new RStructAlreadyExistsError(name, line).raise();
            return;
        }

        cctx.addStruct(false, name, type, fields);
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
}
