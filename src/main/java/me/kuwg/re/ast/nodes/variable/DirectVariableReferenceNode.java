package me.kuwg.re.ast.nodes.variable;

import me.kuwg.re.ast.nodes.pointer.DereferenceNode;
import me.kuwg.re.ast.nodes.struct.StructFieldAccessNode;
import me.kuwg.re.compiler.CompilationContext;
import me.kuwg.re.compiler.struct.RDefaultStruct;
import me.kuwg.re.compiler.variable.RVariable;
import me.kuwg.re.error.errors.variable.RVariableNotFoundError;
import me.kuwg.re.type.TypeRef;
import me.kuwg.re.type.iterable.arr.ArrayType;

import java.util.Map;

public class DirectVariableReferenceNode extends VariableReference {
    private final String name;

    public DirectVariableReferenceNode(final String fileName, final int line, final String name) {
        super(fileName, line);
        this.name = name;
    }

    @Override
    public void replaceGenerics(final Map<String, TypeRef> generics, final CompilationContext cctx) {
    }

    @Override
    public String compileAndGet(final CompilationContext cctx) {
        RVariable var = getVariable(cctx);

        if (var == null) {
            return new RVariableNotFoundError(name, fileName, line).raise();
        }

        setType(var.type());

        if (var.type() instanceof ArrayType && var.addrReg().startsWith("@GLOBAL$")) {
            String loaded = cctx.nextRegister();

            cctx.emit(loaded + " = load " + var.type().getLLVMName() + ", " + toPtr(var.type().getLLVMName()) + var.addrReg());

            return loaded;
        }

        if (!var.addressBacked()) return var.valueReg();

        String llvmType = var.type().getLLVMName();
        String tmp = cctx.nextRegister();

        cctx.emit("; Direct variable reference");
        cctx.emit(tmp + " = load " + llvmType + ", " + toPtr(llvmType) + var.addrReg());

        return tmp;
    }

    @Override
    public void compile(final CompilationContext cctx) {
        compileAndGet(cctx);
    }

    @Override
    public void write(final StringBuilder sb, final String indent) {
        sb.append(indent).append("Name: ").append(name).append(NEWLINE);
    }

    @Override
    public RVariable getVariable(final CompilationContext cctx) {
        var v = cctx.getVariable(name);

        if (v != null && v.addrReg().startsWith("@GLOBAL$")) v = loadGlobalVariable(v, cctx);

        if (v != null) setType(v.type());
        if (v == null) {
            var local = cctx.getVariable("self");
            if (local == null) return null;
            return compileSelfReference(cctx);
        }

        return v;
    }

    private RVariable loadGlobalVariable(final RVariable v, final CompilationContext cctx) {
        if (v.type() instanceof ArrayType) {
            return v;
        }

        String loaded = cctx.nextRegister();

        cctx.emit(loaded + " = load " + v.type().getLLVMName() + ", " + toPtr(v.type().getLLVMName()) + v.addrReg());

        return new RVariable(v.name(), v.mutable(), false, v.type(), v.addrReg(), loaded);
    }

    private RVariable compileSelfReference(CompilationContext cctx) {
        var self = new DereferenceNode(fileName, line, new DirectVariableReferenceNode(fileName, line, "self"));

        cctx.pushFunctionBody();
        self.compileAndGet(cctx);
        cctx.popFunctionBody();

        TypeRef t = self.getType();

        if (!t.toString().startsWith("struct ")) {
            return null;
        }

        RDefaultStruct struct = cctx.getStruct(t.getName());
        if (struct == null || struct.fields().stream().noneMatch(f -> f.name().equals(name))) return null;

        var node = new StructFieldAccessNode(fileName, line, new DereferenceNode(fileName, line, new DirectVariableReferenceNode(fileName, line, "self")), name);
        var res = node.getVariable(cctx);
        setType(node.getType());

        return res;
    }

    @Override
    public String getCompleteName() {
        return name;
    }

    @Override
    public String getSimpleName() {
        return name;
    }

    @Override
    public String toString() {
        return "DirectVariableReferenceNode{" + "name='" + name + '\'' + '}';
    }

    @Override
    public DirectVariableReferenceNode clone() {
        return new DirectVariableReferenceNode(fileName, line, name);
    }
}
