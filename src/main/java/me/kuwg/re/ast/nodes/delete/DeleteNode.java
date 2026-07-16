package me.kuwg.re.ast.nodes.delete;

import me.kuwg.re.ast.ASTNode;
import me.kuwg.re.ast.nodes.variable.VariableReference;
import me.kuwg.re.compiler.CompilationContext;
import me.kuwg.re.compiler.struct.RDefaultStruct;
import me.kuwg.re.compiler.variable.RVariable;
import me.kuwg.re.error.errors.delete.RDeleteTypeError;
import me.kuwg.re.error.errors.struct.RStructUndefinedError;
import me.kuwg.re.error.errors.variable.RVariableNotFoundError;
import me.kuwg.re.type.TypeRef;
import me.kuwg.re.type.iterable.arr.ArrayType;
import me.kuwg.re.type.struct.StructType;

import java.util.Map;

public class DeleteNode extends ASTNode {
    private final VariableReference value;

    public DeleteNode(final String fileName, final int line, final VariableReference value) {
        super(fileName, line);
        this.value = value;
    }

    @Override
    public ASTNode clone() {
        return new DeleteNode(fileName, line, value.clone());
    }

    @Override
    public void replaceGenerics(final Map<String, TypeRef> generics, final CompilationContext cctx) {
        value.replaceGenerics(generics, cctx);
    }

    @Override
    public void compile(final CompilationContext cctx) {
        RVariable variable = value.getVariable(cctx);
        if (variable == null) {
            new RVariableNotFoundError(value.getCompleteName(), fileName, line).raise();
            return;
        }

        TypeRef type = cctx.resolveConcrete(variable.type());
        cctx.emit("; Delete " + value.getCompleteName());

        if (type.isPointer()) {
            String ptrReg = variable.valueReg();

            if (variable.addressBacked()) {
                String loaded = cctx.nextRegister();
                cctx.emit(loaded + " = load " + type.getLLVMName() + ", " + type.getLLVMName() + "* " + variable.addrReg());
                ptrReg = loaded;
            }


            cctx.emit("call void @free(ptr " + ptrReg + ")");
            return;
        }

        if (type instanceof ArrayType at) {
            if (at.isStatic()) {
                new RDeleteTypeError("cannot delete a static array", fileName, line).raise();
                return;
            }

            String ptrReg = variable.valueReg();

            if (variable.addressBacked()) {
                String loaded = cctx.nextRegister();
                cctx.emit(loaded + " = load " + type.getLLVMName() + ", " + type.getLLVMName() + "* " + variable.addrReg());
                ptrReg = loaded;
            }

            cctx.emit("call void @free(ptr " + ptrReg + ")");
        } else if (type instanceof StructType st) {
            RDefaultStruct struct = cctx.getStruct(st.getName());

            if (struct == null) {
                new RStructUndefinedError(st.getName(), fileName, line).raise();
                return;
            }

            if (struct.getDestructor() != null) {
                cctx.emit("call void @" + struct.getDestructor().llvmName() + "(ptr " + variable.addrReg() + ")");
                cctx.unregisterDestructor(variable.addrReg());
            }

        } else {
            new RDeleteTypeError("delete is only supported for pointers, arrays, and structs", fileName, line).raise();
        }
    }

    @Override
    public void write(final StringBuilder sb, final String indent) {
        sb.append(indent).append("Delete:").append(NEWLINE);
        value.write(sb, indent + TAB);
    }
}
