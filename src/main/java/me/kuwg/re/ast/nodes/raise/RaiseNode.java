package me.kuwg.re.ast.nodes.raise;

import me.kuwg.re.ast.ASTNode;
import me.kuwg.re.ast.nodes.constants.NumberNode;
import me.kuwg.re.ast.nodes.constants.StringNode;
import me.kuwg.re.ast.nodes.function.call.FunctionCallNode;
import me.kuwg.re.ast.nodes.function.call.StructFunctionCallNode;
import me.kuwg.re.ast.nodes.statement.TryCatchNode;
import me.kuwg.re.ast.nodes.variable.DirectVariableReferenceNode;
import me.kuwg.re.ast.nodes.variable.VariableDeclarationNode;
import me.kuwg.re.ast.types.interrupt.InterruptNode;
import me.kuwg.re.ast.types.value.ValueNode;
import me.kuwg.re.compiler.CompilationContext;
import me.kuwg.re.compiler.struct.RDefaultStruct;
import me.kuwg.re.error.errors.RInternalError;
import me.kuwg.re.error.errors.variable.RVariableTypeError;
import me.kuwg.re.type.TypeRef;
import me.kuwg.re.type.builtin.BuiltinTypes;
import me.kuwg.re.type.struct.StructType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class RaiseNode extends ASTNode implements InterruptNode {
    private final ValueNode value;

    public RaiseNode(final String fileName, final int line, final ValueNode value) {
        super(fileName, line);
        this.value = value;
    }

    @Override
    public void replaceGenerics(final Map<String, TypeRef> generics, final CompilationContext cctx) {
        value.replaceGenerics(generics, cctx);
    }

    @Override
    public void compile(final CompilationContext cctx) {
        var catches = cctx.popTryCatchScope();

        cctx.emit("; Raise");
        if (catches == null) {
            compileRaise(cctx);
        } else {
            ValueNode cloned = value.clone();
            final String valueReg = cloned.compileAndGet(cctx);
            TypeRef type = cloned.getType();
            TryCatchNode.CompiledCatch matched = null;
            for (TryCatchNode.CompiledCatch cc : catches) {
                if (cc.type() == null) {
                    matched = cc;
                    break;
                } else if (cc.type().isCompatibleWith(type)) {
                    matched = cc;
                    break;
                }
            }
            if (matched == null) {
                compileRaise(cctx);
                return;
            }

            if (matched.variable() != null) {
                new VariableDeclarationNode(
                        fileName, line,
                        new DirectVariableReferenceNode(fileName, line, matched.variable()),
                        true,
                        type,
                        new ValueNode(fileName, line, type) {

                            @Override
                            public void write(final StringBuilder sb, final String indent) {
                                sb.append(indent).append("Caught Value").append(NEWLINE);
                            }

                            @Override
                            public void replaceGenerics(final Map<String, TypeRef> generics, final CompilationContext cctx) {
                            }

                            @Override
                            public void compile(final CompilationContext cctx) {
                                throw new RInternalError("Should be compiled via compileAndGet");
                            }

                            @Override
                            public String compileAndGet(final CompilationContext cctx) {
                                return valueReg;
                            }

                            @Override
                            public ValueNode clone() {
                                return this;
                            }
                        }
                ).compile(cctx);
            }

            cctx.emit("br label %" + matched.label());
        }
    }

    private void compileRaise(CompilationContext cctx) {
        if (value == null) {
            String message = generateLog(line, cctx.writeExceptionLines);

            new FunctionCallNode(
                    fileName, line,
                    "println",
                    List.of(new StringNode(fileName, line, message))
            ).compile(cctx);
        } else {
            ValueNode cloned = value.clone();
            String valueReg = cloned.compileAndGet(cctx);
            TypeRef type = cloned.getType();

            LABEL_O1: {
                if (type == BuiltinTypes.STR.getType()) {
                    compileString(valueReg, cctx);
                    break LABEL_O1;
                } else if (type instanceof StructType st) {
                    RDefaultStruct struct = cctx.getStruct(st.getName());
                    if (struct != null && struct.inherited().contains("Error")) {
                        compileError(cctx);
                        break LABEL_O1;
                    }
                }

                new RVariableTypeError("Raise only supports Error or string types: " + type.getName() + " is not supported", fileName, line).raise();
                return;
            }



        }

        new FunctionCallNode(
                fileName, line,
                "System$$exit",
                List.of(new NumberNode(fileName, line, "1"))
        ).compile(cctx);

        cctx.emit("unreachable");
    }

    private void compileString(String valueReg, CompilationContext cctx) {
        String message = generateLog(line, cctx.writeExceptionLines);

        new FunctionCallNode(
                fileName, line,
                "println",
                List.of(new ValueNode(fileName, line, BuiltinTypes.STR.getType()) {

                    @Override
                    public void write(final StringBuilder sb, final String indent) {
                        sb.append(indent).append("Error Value").append(NEWLINE);
                    }

                    @Override
                    public void replaceGenerics(final Map<String, TypeRef> generics, final CompilationContext cctx) {
                    }

                    @Override
                    public void compile(final CompilationContext cctx) {
                        throw new RInternalError("Should be compiled via compileAndGet");
                    }

                    @Override
                    public String compileAndGet(final CompilationContext cctx) {
                        String strReg = new StringNode(fileName, line, message).compileAndGet(cctx);
                        String msgReg = cctx.nextRegister();

                        cctx.emit(
                                msgReg + " = call i8* @strConcat(i8* " +
                                        strReg +
                                        ", i8* " + valueReg + ")"
                        );

                        return msgReg;
                    }

                    @Override
                    public ValueNode clone() {
                        return this;
                    }
                })
        ).compile(cctx);
    }

    private void compileError(CompilationContext cctx) {
        StructFunctionCallNode messageCall =
                new StructFunctionCallNode(fileName, line, value, "message", new ArrayList<>());

        String messageReg = messageCall.compileAndGet(cctx);

        String errorName = value.getType().getName();

        String nameReg = new StringNode(
                fileName,
                line,
                errorName + ": "
        ).compileAndGet(cctx);

        String fullMessageReg = cctx.nextRegister();

        cctx.emit(
                fullMessageReg + " = call i8* @strConcat(i8* "
                        + nameReg
                        + ", i8* "
                        + messageReg
                        + ")"
        );

        new FunctionCallNode(
                fileName,
                line,
                "println",
                List.of(new ValueNode(fileName, line, BuiltinTypes.STR.getType()) {

                    @Override
                    public void write(StringBuilder sb, String indent) {
                        sb.append(indent).append("Error").append(NEWLINE);
                    }

                    @Override
                    public void replaceGenerics(Map<String, TypeRef> generics, CompilationContext cctx) {
                    }

                    @Override
                    public void compile(CompilationContext cctx) {
                        throw new RInternalError("Should be compiled via compileAndGet");
                    }

                    @Override
                    public String compileAndGet(CompilationContext cctx) {
                        return fullMessageReg;
                    }

                    @Override
                    public ValueNode clone() {
                        return this;
                    }
                })
        ).compile(cctx);
    }

    @Override
    public void write(final StringBuilder sb, final String indent) {
        sb.append(indent).append("Raise").append(NEWLINE);
        if (value != null) {
            value.write(sb, indent + "  ");
        }
    }

    @Override
    public RaiseNode clone() {
        return new RaiseNode(fileName, line, value.clone());
    }

    private static String generateLog(int line, boolean lines) {
        return (!lines ? "An error occurred" : "An error occurred at line " + line) + ".\n";
    }
}