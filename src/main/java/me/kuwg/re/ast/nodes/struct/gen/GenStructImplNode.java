package me.kuwg.re.ast.nodes.struct.gen;

import me.kuwg.re.ast.ASTNode;
import me.kuwg.re.ast.types.global.GlobalNode;
import me.kuwg.re.compiler.CompilationContext;
import me.kuwg.re.compiler.struct.RConstructor;
import me.kuwg.re.compiler.struct.RDefaultStruct;
import me.kuwg.re.compiler.struct.RGenStruct;
import me.kuwg.re.error.errors.struct.RStructUndefinedError;
import me.kuwg.re.type.struct.GenStructType;

import java.util.List;

public class GenStructImplNode extends ASTNode implements GlobalNode {
    private final GenStructType type;
    private final List<String> genericNames;
    private final List<RConstructor> constructors;
    private final List<ASTNode> functions;

    public GenStructImplNode(int line, GenStructType type, List<String> genericNames,
                             List<RConstructor> constructors, List<ASTNode> functions) {
        super(line);
        this.type = type;
        this.genericNames = genericNames;
        this.constructors = constructors;
        this.functions = functions;
    }

    @Override
    public void compile(CompilationContext cctx) {
        RDefaultStruct st = cctx.getStruct(type.name());
        if (!(st instanceof RGenStruct genStruct)) {
            new RStructUndefinedError(type.name(), line).raise();
            return;
        }

        genStruct.addImplTemplate(genericNames, constructors, functions);
    }

    @Override
    public void write(final StringBuilder sb, final String indent) {
        sb.append(indent).append("Gen Struct Impl: ").append(NEWLINE)
                .append(TAB).append("Name: ").append(type.name()).append(NEWLINE)
                .append(TAB).append("Generics: ").append(genericNames).append(NEWLINE)
                .append(TAB).append("Functions:").append(NEWLINE);
        functions.forEach(f -> f.write(sb, indent + TAB + TAB));
    }
}