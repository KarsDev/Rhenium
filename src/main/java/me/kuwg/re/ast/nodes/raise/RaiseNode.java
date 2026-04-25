package me.kuwg.re.ast.nodes.raise;

import me.kuwg.re.ast.ASTNode;
import me.kuwg.re.ast.nodes.constants.NumberNode;
import me.kuwg.re.ast.nodes.constants.StringNode;
import me.kuwg.re.ast.nodes.function.call.FunctionCallNode;
import me.kuwg.re.ast.types.interrupt.InterruptNode;
import me.kuwg.re.ast.types.value.ValueNode;
import me.kuwg.re.compiler.CompilationContext;
import me.kuwg.re.error.errors.variable.RVariableTypeError;
import me.kuwg.re.type.builtin.BuiltinTypes;
import me.kuwg.re.type.builtin.StrBuiltinType;

import java.util.List;

public class RaiseNode extends ASTNode implements InterruptNode {
    private final ValueNode value;

    public RaiseNode(final int line, final ValueNode value) {
        super(line);
        this.value = value;
    }

    @Override
    public void compile(final CompilationContext cctx) {
        String catchLabel = cctx.popTryCatchScope();

        if (catchLabel == null) {

            if (value == null) {
                String message = generateLog(line);

                new FunctionCallNode(
                        line,
                        "println",
                        List.of(new StringNode(line, message))
                ).compile(cctx);
            } else {
                String valueReg = value.compileAndGet(cctx);

                if (!(value.getType() instanceof StrBuiltinType)) {
                    new RVariableTypeError("str", value.getType().getName(), line).raise();
                    return;
                }

                String message = generateLog(line);

                new FunctionCallNode(
                        line,
                        "println",
                        List.of(new ValueNode(line, BuiltinTypes.STR.getType()) {

                            @Override
                            public void write(final StringBuilder sb, final String indent) {
                                sb.append(indent).append("Error Value").append(NEWLINE);
                            }

                            @Override
                            public void compile(final CompilationContext cctx) {
                                throw new RuntimeException("Should be compiled via compileAndGet");
                            }

                            @Override
                            public String compileAndGet(final CompilationContext cctx) {
                                String strReg = new StringNode(line, message).compileAndGet(cctx);
                                String msgReg = cctx.nextRegister();

                                cctx.include(-1, null, "string", null);

                                cctx.emit(
                                        msgReg + " = call i8* @strConcat(i8* " +
                                                strReg +
                                                ", i8* " + valueReg + ")"
                                );

                                return msgReg;
                            }
                        })
                ).compile(cctx);
            }

            new FunctionCallNode(
                    line,
                    "exit",
                    List.of(new NumberNode(line, "1"))
            ).compile(cctx);

            cctx.emit("unreachable");
        } else {
            cctx.emit("br label %" + catchLabel);
        }
    }

    @Override
    public void write(final StringBuilder sb, final String indent) {
        sb.append(indent).append("Raise").append(NEWLINE);
        if (value != null) {
            value.write(sb, indent + "  ");
        }
    }

    private static String generateLog(int line) {
        return "An error occurred at line " + line + ".\n";
    }
}
