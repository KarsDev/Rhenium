package me.kuwg.re.ast.nodes.struct;

import me.kuwg.re.ast.ASTNode;
import me.kuwg.re.ast.types.global.GlobalNode;
import me.kuwg.re.compiler.CompilationContext;
import me.kuwg.re.compiler.variable.RStructField;
import me.kuwg.re.type.struct.StructType;

import java.util.List;

public class StructDeclarationNode extends ASTNode implements GlobalNode {
    private final boolean builtin;
    private final String name;
    private final StructType type;
    private final List<RStructField> fields;

    public StructDeclarationNode(final int line, final boolean builtin, final String name, final StructType type, final List<RStructField> fields) {
        super(line);
        this.builtin = builtin;
        this.name = name;
        this.type = type;
        this.fields = fields;
    }

    @Override
    public void compile(final CompilationContext cctx) {
        cctx.addStruct(builtin, name, type, fields);

        StringBuilder sb = new StringBuilder();
        sb.append("; Struct declaration\n");
        sb.append("%struct.").append(name).append(" = type { ");

        for (int i = 0; i < fields.size(); i++) {
            sb.append(fields.get(i).type().getLLVMName());
            if (i + 1 < fields.size()) sb.append(", ");
        }

        sb.append(" }");

        cctx.declare(sb.toString());
    }

    @Override
    public void write(final StringBuilder sb, final String indent) {
        sb.append(indent).append("Struct Declaration: ").append(NEWLINE);
        sb.append(indent).append(TAB).append("Name: ").append(name).append(NEWLINE);
        sb.append(indent).append(TAB).append("Fields: ").append(NEWLINE);
        for (final RStructField field : fields) {
            sb.append(indent).append(TAB).append(TAB).append("Name: ").append(field.name()).append(", Type: ")
                    .append(field.type().getName()).append(NEWLINE);
        }
    }
}
