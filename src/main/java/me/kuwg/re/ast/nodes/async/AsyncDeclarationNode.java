package me.kuwg.re.ast.nodes.async;

import me.kuwg.re.ast.nodes.blocks.BlockNode;
import me.kuwg.re.ast.types.value.ValueNode;
import me.kuwg.re.compiler.CompilationContext;
import me.kuwg.re.error.errors.value.RValueMustBeUsedError;
import me.kuwg.re.type.TypeRef;
import me.kuwg.re.type.builtin.BuiltinTypes;
import me.kuwg.re.type.builtin.NoneBuiltinType;
import me.kuwg.re.type.struct.StructType;

import java.util.List;

public class AsyncDeclarationNode extends ValueNode {
    private static final TypeRef THREAD_TYPE = new StructType("Thread", List.of(BuiltinTypes.ANYPTR.getType()), null);

    private final TypeRef returnType;
    private final BlockNode block;

    public AsyncDeclarationNode(final int line, final TypeRef returnType, final BlockNode block) {
        super(line, THREAD_TYPE);
        this.returnType = returnType;
        this.block = block;
    }

    @Override
    public String compileAndGet(final CompilationContext cctx) {
        String wrapperName = "_async_wrapper_" + cctx.nextLabel("");

        cctx.pushFunctionBody();
        cctx.pushScope();
        cctx.pushIndent();

        if (!(returnType instanceof NoneBuiltinType)) {
            String retVal = block.compileAndGet(returnType, cctx);

            String resultPtrHeap = cctx.nextRegister();
            cctx.emit(resultPtrHeap + " = call i8* @malloc(i64 " + returnType.getSize() + ")");
            String typedPtr = cctx.nextRegister();
            cctx.emit(typedPtr + " = bitcast i8* " + resultPtrHeap + " to " + returnType.getLLVMName() + "*");

            cctx.emit("store " + returnType.getLLVMName() + " " + retVal + ", " + returnType.getLLVMName() + "* " + typedPtr);
            cctx.emit("ret i8* " + resultPtrHeap);
        } else {
            block.compile(cctx);
            cctx.emit("ret i8* null");
        }

        block.checkTypes(returnType, true);

        String body = cctx.popFunctionBody();
        cctx.popScope();
        cctx.popIndent();

        cctx.declare("define i8* @" + wrapperName + "(i8* %raw_ctx) {\nentry:\n" + body + "\n}\n");

        cctx.addIR("declare i8* @rhenium_spawn(i8* (i8*)*, i8*)");

        String rawHandle = cctx.nextRegister();
        cctx.emit(rawHandle + " = call i8* @rhenium_spawn(i8* (i8*)* @" + wrapperName + ", i8* null)");

        String structPtr = cctx.nextRegister();
        cctx.emit(structPtr + " = alloca %struct.Thread");
        String fieldPtr = cctx.nextRegister();
        cctx.emit(fieldPtr + " = getelementptr %struct.Thread, %struct.Thread* " + structPtr + ", i32 0, i32 0");
        cctx.emit("store i8* " + rawHandle + ", i8** " + fieldPtr);

        return structPtr;
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
