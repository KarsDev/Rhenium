package me.kuwg.re.ast.nodes.loop;

import me.kuwg.re.ast.ASTNode;
import me.kuwg.re.ast.nodes.blocks.BlockNode;
import me.kuwg.re.ast.nodes.blocks.IBlockContainer;
import me.kuwg.re.ast.value.ValueNode;
import me.kuwg.re.compiler.CompilationContext;
import me.kuwg.re.compiler.LoopContext;
import me.kuwg.re.compiler.variable.RVariable;
import me.kuwg.re.error.errors.loop.RValueIsNotIterableError;
import me.kuwg.re.error.errors.variable.RVariableAlreadyExistsError;
import me.kuwg.re.error.errors.variable.RVariableTypeError;
import me.kuwg.re.type.TypeRef;
import me.kuwg.re.type.arr.ArrayType;
import me.kuwg.re.type.builtin.BuiltinTypes;
import me.kuwg.re.type.iterable.IterableTypeRef;
import me.kuwg.re.type.iterable.RangeType;

public class ForLoopNode extends ASTNode implements IBlockContainer {
    private final String variable;
    private final ValueNode collection;
    private final BlockNode block;

    public ForLoopNode(final int line, final String variable, final ValueNode collection, final BlockNode block) {
        super(line);
        this.variable = variable;
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

    @Override
    public void write(final StringBuilder sb, final String indent) {
        sb.append(indent).append("For Loop: ").append(NEWLINE);
        sb.append(indent).append(TAB).append("Variable: ").append(variable).append(NEWLINE);
        sb.append(indent).append(TAB).append("Collection: ").append(NEWLINE);
        collection.write(sb, indent + TAB + TAB);
        block.write(sb, indent + TAB);
    }

    private void compileRange(CompilationContext cctx, RangeType range) {
        cctx.pushScope();

        String startReg = cctx.nextRegister();
        String endReg = cctx.nextRegister();
        String stepReg = cctx.nextRegister();

        cctx.emit(startReg + " = alloca i32, align 4 ; allocate start");
        cctx.emit(endReg + " = alloca i32, align 4 ; allocate end");
        cctx.emit(stepReg + " = alloca i32, align 4 ; allocate step");

        String startVal = range.start().compileAndGet(cctx);
        String endVal = range.end().compileAndGet(cctx);
        String stepVal = range.step().compileAndGet(cctx);

        cctx.emit("store i32 " + startVal + ", i32* " + startReg + " ; initialize start");
        cctx.emit("store i32 " + endVal + ", i32* " + endReg + " ; initialize end");
        cctx.emit("store i32 " + stepVal + ", i32* " + stepReg + " ; initialize step");

        if (!BuiltinTypes.INT.getType().isCompatibleWith(range.start().getType())) {
            new RVariableTypeError(range.start().getType().getName(), "int", line).raise();
        }
        if (!BuiltinTypes.INT.getType().isCompatibleWith(range.end().getType())) {
            new RVariableTypeError(range.end().getType().getName(), "int", line).raise();
        }
        if (!BuiltinTypes.INT.getType().isCompatibleWith(range.step().getType())) {
            new RVariableTypeError(range.step().getType().getName(), "int", line).raise();
        }

        cctx.emit('%' + variable + " = alloca i32 ; allocate loop variable");

        String startLoaded = cctx.nextRegister();
        cctx.emit(startLoaded + " = load i32, i32* " + startReg);
        cctx.emit("store i32 " + startLoaded + ", i32* %" + variable + " ; initialize loop variable");

        cctx.addVariable(new RVariable(variable, true, BuiltinTypes.INT.getType(), "%" + variable));

        String startLabel = cctx.nextLabel("for_start");
        String bodyLabel = cctx.nextLabel("for_body");
        String endLabel = cctx.nextLabel("for_end");

        cctx.getLoopStack().push(new LoopContext(startLabel, bodyLabel, endLabel));

        cctx.emit("br label %" + startLabel);

        cctx.emit(startLabel + ":");
        String currentReg = cctx.nextRegister();
        cctx.emit(currentReg + " = load i32, i32* %" + variable + " ; load current loop variable");

        String endLoaded = cctx.nextRegister();
        String stepLoaded = cctx.nextRegister();
        cctx.emit(endLoaded + " = load i32, i32* " + endReg + " ; load end value");
        cctx.emit(stepLoaded + " = load i32, i32* " + stepReg + " ; load step value");

        String condReg = cctx.nextRegister();
        cctx.emit(condReg + " = icmp slt i32 " + currentReg + ", " + endLoaded + " ; loop condition");

        cctx.emit("br i1 " + condReg + ", label %" + bodyLabel + ", label %" + endLabel);

        cctx.emit(bodyLabel + ":");
        cctx.pushIndent();
        block.compile(cctx);
        cctx.popIndent();

        String updatedReg = cctx.nextRegister();
        cctx.emit(updatedReg + " = add i32 " + currentReg + ", " + stepLoaded + " ; increment loop variable");
        cctx.emit("store i32 " + updatedReg + ", i32* %" + variable + " ; update loop variable");

        cctx.emit("br label %" + startLabel);

        cctx.emit(endLabel + ":");

        cctx.getLoopStack().pop();
        cctx.popScope();
    }

    private void compileArray(CompilationContext cctx, ArrayType arr, String reg) {
        cctx.pushScope();

        String indexReg = cctx.nextRegister();
        cctx.emit(indexReg + " = alloca i32 ; loop index");
        cctx.emit("store i32 0, i32* " + indexReg + " ; initialize index");

        String elemTypeLLVM = arr.inner().getLLVMName();
        cctx.emit('%' + variable + " = alloca " + elemTypeLLVM + " ; allocate loop variable");
        cctx.addVariable(new RVariable(variable, true, arr.inner(), "%" + variable));

        String startLabel = cctx.nextLabel("for_start");
        String bodyLabel = cctx.nextLabel("for_body");
        String endLabel = cctx.nextLabel("for_end");

        cctx.getLoopStack().push(new LoopContext(startLabel, bodyLabel, endLabel));

        cctx.emit("br label %" + startLabel);

        cctx.emit(startLabel + ":");
        String currentIndex = cctx.nextRegister();
        cctx.emit(currentIndex + " = load i32, i32* " + indexReg + " ; load current index");

        String condReg = cctx.nextRegister();
        cctx.emit(condReg + " = icmp slt i32 " + currentIndex + ", " + arr.size() + " ; index < size");

        cctx.emit("br i1 " + condReg + ", label %" + bodyLabel + ", label %" + endLabel);

        cctx.emit(bodyLabel + ":");
        cctx.pushIndent();

        String elemPtr = cctx.nextRegister();
        String llvmArrType = "[" + arr.size() + " x " + elemTypeLLVM + "]";
        cctx.emit(elemPtr + " = getelementptr " + llvmArrType + ", " + llvmArrType + "* " + reg + ", i32 0, i32 " + currentIndex + " ; element ptr");
        String elemValue = cctx.nextRegister();
        cctx.emit(elemValue + " = load " + elemTypeLLVM + ", " + elemTypeLLVM + "* " + elemPtr + " ; load element");

        cctx.emit("store " + elemTypeLLVM + " " + elemValue + ", " + elemTypeLLVM + "* %" + variable + " ; assign to loop variable");

        block.compile(cctx);
        cctx.popIndent();

        String nextIndex = cctx.nextRegister();
        cctx.emit(nextIndex + " = add i32 " + currentIndex + ", 1 ; increment index");
        cctx.emit("store i32 " + nextIndex + ", i32* " + indexReg + " ; update index");

        cctx.emit("br label %" + startLabel);
        cctx.emit(endLabel + ":");

        cctx.getLoopStack().pop();
        cctx.popScope();
    }
}
