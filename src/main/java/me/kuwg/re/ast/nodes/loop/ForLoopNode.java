package me.kuwg.re.ast.nodes.loop;

import me.kuwg.re.ast.ASTNode;
import me.kuwg.re.ast.nodes.blocks.BlockNode;
import me.kuwg.re.ast.nodes.blocks.IBlockContainer;
import me.kuwg.re.ast.types.value.ValueNode;
import me.kuwg.re.compiler.CompilationContext;
import me.kuwg.re.compiler.LoopContext;
import me.kuwg.re.compiler.variable.RVariable;
import me.kuwg.re.error.errors.loop.RValueIsNotIterableError;
import me.kuwg.re.error.errors.variable.RVariableAlreadyExistsError;
import me.kuwg.re.type.TypeRef;
import me.kuwg.re.type.builtin.BuiltinTypes;
import me.kuwg.re.type.iterable.IterableTypeRef;
import me.kuwg.re.type.iterable.arr.ArrayType;
import me.kuwg.re.type.iterable.range.RangeType;

public class ForLoopNode extends ASTNode implements IBlockContainer {
    private final String variable;
    private final String llvmVariable;
    private final ValueNode collection;
    private final BlockNode block;

    public ForLoopNode(final int line, final String variable, final ValueNode collection, final BlockNode block) {
        super(line);
        this.variable = variable;
        this.llvmVariable = RVariable.makeUnique(variable);
        this.collection = collection;
        this.block = block;
    }

    @Override
    public BlockNode getBlock() {
        return block;
    }

    @Override
    public void compile(final CompilationContext cctx) {
        String reg = collection.compileAndGet(cctx);
        TypeRef type = collection.getType();

        if (!(type instanceof IterableTypeRef)) {
            new RValueIsNotIterableError(variable, line).raise();
            return;
        }

        if (cctx.getVariable(variable) != null) {
            new RVariableAlreadyExistsError(variable, line).raise();
            return;
        }

        if (type instanceof RangeType range) {
            compileRange(cctx, range);
            return;
        }

        if (type instanceof ArrayType arr) {
            compileArray(cctx, arr, reg);
            return;
        }

        new RValueIsNotIterableError(variable, line).raise();
    }

    private void compileRange(CompilationContext cctx, RangeType range) {
        cctx.pushScope();

        String startReg = cctx.nextRegister();
        String endReg = cctx.nextRegister();
        String stepReg = cctx.nextRegister();

        cctx.emit(startReg + " = alloca i32, align 4");
        cctx.emit(endReg + " = alloca i32, align 4");
        cctx.emit(stepReg + " = alloca i32, align 4");

        String startVal = range.start().compileAndGet(cctx);
        String endVal = range.end().compileAndGet(cctx);
        String stepVal = range.step().compileAndGet(cctx);

        cctx.emit("store i32 " + startVal + ", i32* " + startReg);
        cctx.emit("store i32 " + endVal + ", i32* " + endReg);
        cctx.emit("store i32 " + stepVal + ", i32* " + stepReg);

        cctx.emit('%' + llvmVariable + " = alloca i32");
        String startLoaded = cctx.nextRegister();
        cctx.emit(startLoaded + " = load i32, i32* " + startReg);
        cctx.emit("store i32 " + startLoaded + ", i32* %" + llvmVariable);

        cctx.addVariable(new RVariable(variable, true, BuiltinTypes.INT.getType(), "%" + llvmVariable));

        String startLabel = cctx.nextLabel("for_start");
        String bodyLabel = cctx.nextLabel("for_body");
        String incLabel   = cctx.nextLabel("for_inc");
        String endLabel   = cctx.nextLabel("for_end");

        cctx.getLoopStack().push(new LoopContext(incLabel, bodyLabel, endLabel));

        cctx.emit("br label %" + startLabel);

        cctx.emit(startLabel + ":");
        String currentReg = cctx.nextRegister();
        cctx.emit(currentReg + " = load i32, i32* %" + llvmVariable);
        String endLoaded = cctx.nextRegister();
        cctx.emit(endLoaded + " = load i32, i32* " + endReg);
        String condReg = cctx.nextRegister();
        cctx.emit(condReg + " = icmp slt i32 " + currentReg + ", " + endLoaded);
        cctx.emit("br i1 " + condReg + ", label %" + bodyLabel + ", label %" + endLabel);

        cctx.emit(bodyLabel + ":");
        cctx.pushIndent();
        block.compile(cctx);
        cctx.emit("br label %" + incLabel);
        cctx.popIndent();

        cctx.emit(incLabel + ":");
        String currForInc = cctx.nextRegister();
        cctx.emit(currForInc + " = load i32, i32* %" + llvmVariable);
        String stepLoaded = cctx.nextRegister();
        cctx.emit(stepLoaded + " = load i32, i32* " + stepReg);
        String updatedReg = cctx.nextRegister();
        cctx.emit(updatedReg + " = add i32 " + currForInc + ", " + stepLoaded);
        cctx.emit("store i32 " + updatedReg + ", i32* %" + llvmVariable);
        cctx.emit("br label %" + startLabel);

        cctx.emit(endLabel + ":");

        cctx.getLoopStack().pop();
        cctx.popScope();
    }

    private void compileArray(CompilationContext cctx, ArrayType arr, String reg) {
        cctx.pushScope();

        String indexReg = cctx.nextRegister();
        cctx.emit(indexReg + " = alloca i32");
        cctx.emit("store i32 0, i32* " + indexReg);

        String elemTypeLLVM = arr.inner().getLLVMName();
        cctx.emit('%' + llvmVariable + " = alloca " + elemTypeLLVM);
        cctx.addVariable(new RVariable(variable, true, arr.inner(), "%" + llvmVariable));

        String startLabel = cctx.nextLabel("for_start");
        String bodyLabel = cctx.nextLabel("for_body");
        String incLabel   = cctx.nextLabel("for_inc");
        String endLabel   = cctx.nextLabel("for_end");

        cctx.getLoopStack().push(new LoopContext(incLabel, bodyLabel, endLabel));

        cctx.emit("br label %" + startLabel);

        cctx.emit(startLabel + ":");
        String currentIndex = cctx.nextRegister();
        cctx.emit(currentIndex + " = load i32, i32* " + indexReg);
        String condReg = cctx.nextRegister();
        cctx.emit(condReg + " = icmp slt i32 " + currentIndex + ", " + arr.size());
        cctx.emit("br i1 " + condReg + ", label %" + bodyLabel + ", label %" + endLabel);

        cctx.emit(bodyLabel + ":");
        cctx.pushIndent();
        String elemPtr = cctx.nextRegister();
        String llvmArrType = "[" + arr.size() + " x " + elemTypeLLVM + "]";
        cctx.emit(elemPtr + " = getelementptr " + llvmArrType + ", " + llvmArrType + "* " + reg + ", i32 0, i32 " + currentIndex);
        String elemValue = cctx.nextRegister();
        cctx.emit(elemValue + " = load " + elemTypeLLVM + ", " + elemTypeLLVM + "* " + elemPtr);
        cctx.emit("store " + elemTypeLLVM + " " + elemValue + ", " + elemTypeLLVM + "* %" + llvmVariable);

        block.compile(cctx);
        cctx.emit("br label %" + incLabel);
        cctx.popIndent();

        cctx.emit(incLabel + ":");
        String idxForInc = cctx.nextRegister();
        cctx.emit(idxForInc + " = load i32, i32* " + indexReg);
        String nextIndex = cctx.nextRegister();
        cctx.emit(nextIndex + " = add i32 " + idxForInc + ", 1");
        cctx.emit("store i32 " + nextIndex + ", i32* " + indexReg);
        cctx.emit("br label %" + startLabel);

        cctx.emit(endLabel + ":");

        cctx.getLoopStack().pop();
        cctx.popScope();
    }

    @Override
    public void write(final StringBuilder sb, final String indent) {
        sb.append(indent).append("For Loop: ").append(NEWLINE);
        sb.append(indent).append(TAB).append("Variable: ").append(variable).append(NEWLINE);
        sb.append(indent).append(TAB).append("Collection: ").append(NEWLINE);
        collection.write(sb, indent + TAB + TAB);
        block.write(sb, indent + TAB);
    }
}