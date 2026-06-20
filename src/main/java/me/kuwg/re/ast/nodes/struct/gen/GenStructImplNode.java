package me.kuwg.re.ast.nodes.struct.gen;

import me.kuwg.re.ast.ASTNode;
import me.kuwg.re.ast.types.global.GlobalNode;
import me.kuwg.re.compiler.CompilationContext;
import me.kuwg.re.compiler.generic.TypeParameter;
import me.kuwg.re.compiler.struct.RConstructor;
import me.kuwg.re.compiler.struct.RDefaultStruct;
import me.kuwg.re.compiler.struct.RGenStruct;
import me.kuwg.re.error.errors.struct.RStructUndefinedError;
import me.kuwg.re.type.TypeRef;
import me.kuwg.re.type.struct.GenStructType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

public class GenStructImplNode extends ASTNode implements GlobalNode {
    private final GenStructType type;
    private final List<TypeParameter> genericNames;
    private final List<RConstructor> constructors;
    private final List<ASTNode> functions;

    public GenStructImplNode(final String fileName, int line, GenStructType type, List<TypeParameter> genericNames,
                             List<RConstructor> constructors, List<ASTNode> functions) {
        super(fileName, line);
        this.type = type;
        this.genericNames = genericNames;
        this.constructors = constructors;
        this.functions = functions;
    }

    @Override
    public void replaceGenerics(final Map<String, TypeRef> generics, final CompilationContext cctx) {
    }

    @Override
    public void compile(CompilationContext cctx) {
        cctx.declare("; Generic Struct impl");

        RDefaultStruct st = cctx.getStruct(type.name());
        if (!(st instanceof RGenStruct genStruct)) {
            new RStructUndefinedError(type.name(), fileName, line).raise();
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

    @Override
    public GenStructImplNode clone() {
        List<RConstructor> constructorsCloned = new ArrayList<>();
        IntStream.range(0, constructors.size()).forEach(i -> constructorsCloned.add(i, constructors.get(i).clone()));

        List<ASTNode> functionsCloned = new ArrayList<>();
        IntStream.range(0, functions.size()).forEach(i -> functionsCloned.add(i, functions.get(i).clone()));

        return new GenStructImplNode(fileName, line, type, genericNames, constructorsCloned, functionsCloned);
    }
}