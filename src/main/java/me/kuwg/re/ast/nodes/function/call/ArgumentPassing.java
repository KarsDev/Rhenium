package me.kuwg.re.ast.nodes.function.call;

import me.kuwg.re.ast.nodes.struct.StructFieldAccessNode;
import me.kuwg.re.ast.nodes.variable.DirectVariableReferenceNode;
import me.kuwg.re.ast.nodes.variable.VariableReference;
import me.kuwg.re.ast.types.value.ValueNode;
import me.kuwg.re.compiler.CompilationContext;
import me.kuwg.re.compiler.variable.RVariable;
import me.kuwg.re.type.TypeRef;

final class ArgumentPassing {
    private ArgumentPassing() {
    }

    static String addressOf(final CompilationContext cctx, final ValueNode node, final String valueReg, final TypeRef structType, final boolean castApplied) {
        if (!castApplied && node instanceof VariableReference vr && isAddressable(vr)) {
            RVariable var = vr.getVariable(cctx);
            if (var != null) {
                return var.addrReg();
            }
        }

        String llvm = structType.getLLVMName();
        String tmp = cctx.nextRegister();
        cctx.emit(tmp + " = alloca " + llvm);
        cctx.emit("store " + llvm + " " + valueReg + ", " + llvm + "* " + tmp);
        return tmp;
    }

    private static boolean isAddressable(final VariableReference vr) {
        if (vr instanceof StructFieldAccessNode) return true;
        if (vr instanceof StructFunctionCallNode) return false;
        return vr instanceof DirectVariableReferenceNode;
    }
}