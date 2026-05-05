package me.kuwg.re.ast.nodes.async;

import me.kuwg.re.ast.nodes.blocks.BlockNode;
import me.kuwg.re.ast.nodes.function.declaration.FunctionDeclarationNode;
import me.kuwg.re.ast.types.value.ValueNode;
import me.kuwg.re.compiler.CompilationContext;
import me.kuwg.re.error.errors.value.RValueMustBeUsedError;
import me.kuwg.re.type.TypeRef;
import me.kuwg.re.type.builtin.BuiltinTypes;
import me.kuwg.re.type.struct.StructType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AsyncDeclarationNode extends ValueNode {
    private static final TypeRef THREAD_TYPE = new StructType("Thread", List.of(BuiltinTypes.ANYPTR.getType()));

    private TypeRef returnType;
    private final BlockNode block;

    public AsyncDeclarationNode(final int line, final TypeRef returnType, final BlockNode block) {
        super(line, THREAD_TYPE);
        this.returnType = returnType;
        this.block = block;
    }

    @Override
    public void replaceGenerics(final Map<String, TypeRef> generics) {
        returnType = replaceGenericType(returnType, generics);
        block.replaceGenerics(generics);
    }

    @Override
    public String compileAndGet(final CompilationContext cctx) {
        final String wrapperName = "AsyncCompilationFunction" + cctx.nextRegister().substring(1);
        final String innerName = wrapperName + "_inner";

        final FunctionDeclarationNode innerFn = new FunctionDeclarationNode(
                line,
                false,
                innerName,
                new ArrayList<>(),
                returnType,
                block.clone()
        );
        innerFn.compile(cctx);

        cctx.declare("define i8* @" + wrapperName + "() {");
        cctx.declare("entry:");

        final String val = cctx.nextRegister();
        cctx.declare("  " + val + " = call " + returnType.getLLVMName() + " @" + innerFn.getLLVMName() + "()");

        final long size = returnType.getSize();
        final String mem = cctx.nextRegister();
        cctx.declare("  " + mem + " = call i8* @malloc(i64 " + size + ")");

        final String casted = cctx.nextRegister();
        cctx.declare("  " + casted + " = bitcast i8* " + mem + " to " + returnType.getLLVMName() + "*");

        cctx.declare("  store " + returnType.getLLVMName() + " " + val + ", " + returnType.getLLVMName() + "* " + casted);

        cctx.declare("  ret i8* " + mem);
        cctx.declare("}");

        cctx.addIR("declare i8* @rhenium_spawn(i8*)");

        final String fnPtr = cctx.nextRegister();
        cctx.emit(fnPtr + " = bitcast i8* ()* @" + wrapperName + " to i8*");

        final String handle = cctx.nextRegister();
        cctx.emit(handle + " = call i8* @rhenium_spawn(i8* " + fnPtr + ")");

        final String threadValue = cctx.nextRegister();
        cctx.emit(threadValue + " = insertvalue " + THREAD_TYPE.getLLVMName() + " undef, i8* " + handle + ", 0");

        setType(THREAD_TYPE);
        return threadValue;
    }

    @Override
    public void compile(final CompilationContext cctx) {
        new RValueMustBeUsedError("Async Declaration", line).raise();
    }

    @Override
    public void write(final StringBuilder sb, final String indent) {
        sb.append(indent).append("Async Declaration: ").append(NEWLINE);
        block.write(sb, indent + TAB);
    }
}