package me.kuwg.re.compiler.struct;

import me.kuwg.re.ast.nodes.blocks.BlockNode;
import me.kuwg.re.ast.nodes.function.declaration.FunctionParameter;

import java.util.List;

public record RConstructor(String llvmName, List<FunctionParameter> parameters, BlockNode block) implements Cloneable {
    @Override
    public RConstructor clone(){
        try {
            return (RConstructor) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }
}
