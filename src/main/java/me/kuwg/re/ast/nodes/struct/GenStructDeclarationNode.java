package me.kuwg.re.ast.nodes.struct;

import me.kuwg.re.ast.ASTNode;
import me.kuwg.re.compiler.CompilationContext;
import me.kuwg.re.compiler.variable.RStructField;
import me.kuwg.re.error.errors.struct.RStructAlreadyExistsError;
import me.kuwg.re.type.struct.GenStructType;

import java.util.List;

public class GenStructDeclarationNode extends ASTNode {
    private final List<String> genericTypes;
    private final String name;
    private final GenStructType type;
    private final List<RStructField> fields;

    public GenStructDeclarationNode(final int line, final List<String> genericTypes, final String name, final GenStructType type, final List<RStructField> fields) {
        super(line);
        this.genericTypes = genericTypes;
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

        cctx.addGenStruct(name, type, fields);
    }

    @Override
    public void write(final StringBuilder sb, final String indent) {
        sb.append(indent).append("Generic Struct Declaration: ").append(NEWLINE);
        sb.append(indent).append(TAB).append("Generic Types: ").append(genericTypes).append(NEWLINE);
        sb.append(indent).append(TAB).append("Name: ").append(name).append(NEWLINE);
        sb.append(indent).append(TAB).append("Fields: ").append(NEWLINE);
        for (final RStructField field : fields) {
            sb.append(indent).append(TAB).append(TAB).append("Name: ").append(field.name()).append(", Type: ")
                    .append(field.type().getName()).append(NEWLINE);
        }
    }
}
