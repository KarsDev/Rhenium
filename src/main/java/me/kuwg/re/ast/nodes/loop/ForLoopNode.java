package me.kuwg.re.ast.nodes.loop;

import me.kuwg.re.ast.ASTNode;
import me.kuwg.re.ast.nodes.blocks.BlockNode;
import me.kuwg.re.ast.nodes.blocks.IBlockContainer;
import me.kuwg.re.ast.nodes.range.RangeNode;
import me.kuwg.re.ast.nodes.variable.VariableReference;
import me.kuwg.re.ast.types.value.ValueNode;
import me.kuwg.re.compiler.CompilationContext;
import me.kuwg.re.compiler.loop.LoopContext;
import me.kuwg.re.compiler.variable.RVariable;
import me.kuwg.re.error.errors.loop.RValueIsNotIterableError;
import me.kuwg.re.error.errors.variable.RVariableAlreadyExistsError;
import me.kuwg.re.error.errors.variable.RVariableNotFoundError;
import me.kuwg.re.type.TypeRef;
import me.kuwg.re.type.builtin.BuiltinTypes;
import me.kuwg.re.type.iterable.IterableTypeRef;
import me.kuwg.re.type.iterable.arr.ArrayType;
import me.kuwg.re.type.iterable.range.RangeType;
import me.kuwg.re.type.struct.StructType;

import java.util.Map;

public class ForLoopNode extends ASTNode implements IBlockContainer {
    private final String variable;
    private final String llvmVariable;
    private final ValueNode collection;
    private final BlockNode block;

    public ForLoopNode(final String fileName, final int line, final String variable, final ValueNode collection, final BlockNode block) {
        super(fileName, line);
        this.variable = variable;
        this.llvmVariable = RVariable.makeUnique(variable);
        this.collection = collection;
        this.block = block;
    }

    @Override
    public void replaceGenerics(final Map<String, TypeRef> generics, final CompilationContext cctx) {
        collection.replaceGenerics(generics, cctx);
        block.replaceGenerics(generics, cctx);
    }

    @Override
    public BlockNode getBlock() {
        return block;
    }

    @Override
    public void compile(final CompilationContext cctx) {
        cctx.emit("; For loop");

        if (collection instanceof RangeNode) {
            compileRange(cctx, (RangeType) collection.getType());
            return;
        }

        String reg = collection.compileAndGet(cctx);

        if (collection.getType() instanceof ArrayType arr) {
            if (collection instanceof VariableReference varNode) {
                var v = cctx.getVariable(varNode.getSimpleName());
                if (v == null) {
                    new RVariableNotFoundError(varNode.getSimpleName(), fileName, line).raise();
                    return;
                }
                reg = v.addrReg();
            } else {
                String llvmArrType = "[" + arr.size() + " x " + arr.getInner().getLLVMName() + "]";

                String arrayPtr = cctx.nextRegister();
                cctx.emit(arrayPtr + " = alloca " + llvmArrType);
                cctx.emit("store " + llvmArrType + " " + reg + ", " + llvmArrType + "* " + arrayPtr);

                reg = arrayPtr;
            }
        }

        TypeRef type = collection.getType();

        if (!(type instanceof IterableTypeRef)) {
            new RValueIsNotIterableError(type.getName(), fileName, line).raise();
            return;
        }

        if (cctx.getVariable(variable) != null) {
            new RVariableAlreadyExistsError(variable, line, fileName).raise();
            return;
        }

        if (type instanceof ArrayType arr) {
            compileArray(cctx, arr, reg);
            return;
        }

        if (type == BuiltinTypes.STR.getType()) {
            compileStr(cctx, reg);
            return;
        }

        new RValueIsNotIterableError(variable, fileName, line).raise();
    }

    private String widen(final CompilationContext cctx, final String val, final TypeRef from, final String target) {
        final String fromLLVM = from.getLLVMName();
        if (fromLLVM.equals(target)) return val;

        final String ext = cctx.nextRegister();
        cctx.emit(ext + " = sext " + fromLLVM + " " + val + " to " + target);
        return ext;
    }

    private void compileRange(CompilationContext cctx, RangeType range) {
        cctx.pushScope();

        final String start = range.start().compileAndGet(cctx);
        final String end = range.end().compileAndGet(cctx);
        final String step = range.step().compileAndGet(cctx);

        final TypeRef startType = range.start().getType();
        final TypeRef endType = range.end().getType();
        final TypeRef stepType = range.step().getType();

        final boolean useLong = startType == BuiltinTypes.LONG.getType() || endType == BuiltinTypes.LONG.getType() || stepType == BuiltinTypes.LONG.getType();
        final String llvmType = useLong ? "i64" : "i32";
        final int align = useLong ? 8 : 4;
        final TypeRef loopType = useLong ? BuiltinTypes.LONG.getType() : BuiltinTypes.INT.getType();

        String startReg = cctx.nextRegister();
        String endReg = cctx.nextRegister();
        String stepReg = cctx.nextRegister();

        cctx.emit(startReg + " = alloca " + llvmType + ", align " + align);
        cctx.emit(endReg + " = alloca " + llvmType + ", align " + align);
        cctx.emit(stepReg + " = alloca " + llvmType + ", align " + align);

        String startVal = widen(cctx, start, startType, llvmType);
        String endVal = widen(cctx, end, endType, llvmType);
        String stepVal = widen(cctx, step, stepType, llvmType);

        cctx.emit("store " + llvmType + " " + startVal + ", " + llvmType + "* " + startReg);
        cctx.emit("store " + llvmType + " " + endVal + ", " + llvmType + "* " + endReg);
        cctx.emit("store " + llvmType + " " + stepVal + ", " + llvmType + "* " + stepReg);

        cctx.emit('%' + llvmVariable + " = alloca " + llvmType);
        String startLoaded = cctx.nextRegister();
        cctx.emit(startLoaded + " = load " + llvmType + ", " + llvmType + "* " + startReg);
        cctx.emit("store " + llvmType + " " + startLoaded + ", " + llvmType + "* %" + llvmVariable);

        String loopVarAddr = "%" + llvmVariable;

        String loopVarValue = cctx.nextRegister();
        cctx.emit(loopVarValue + " = load " + llvmType + ", " + llvmType + "* " + loopVarAddr);

        cctx.addVariable(new RVariable(variable, true, true, loopType, loopVarAddr, loopVarValue, null));

        String startLabel = cctx.nextLabel("for_start");
        String bodyLabel = cctx.nextLabel("for_body");
        String incLabel = cctx.nextLabel("for_inc");
        String endLabel = cctx.nextLabel("for_end");

        cctx.getLoopStack().push(new LoopContext(incLabel, bodyLabel, endLabel));

        cctx.emit("br label %" + startLabel);

        cctx.emit(startLabel + ":");
        String currentReg = cctx.nextRegister();
        cctx.emit(currentReg + " = load " + llvmType + ", " + llvmType + "* %" + llvmVariable);
        String endLoaded = cctx.nextRegister();
        cctx.emit(endLoaded + " = load " + llvmType + ", " + llvmType + "* " + endReg);
        String condReg = cctx.nextRegister();
        cctx.emit(condReg + " = icmp slt " + llvmType + " " + currentReg + ", " + endLoaded);
        cctx.emit("br i1 " + condReg + ", label %" + bodyLabel + ", label %" + endLabel);

        cctx.emit(bodyLabel + ":");
        cctx.pushIndent();
        block.compile(cctx);
        cctx.emit("br label %" + incLabel);
        cctx.popIndent();

        cctx.emit(incLabel + ":");
        String currForInc = cctx.nextRegister();
        cctx.emit(currForInc + " = load " + llvmType + ", " + llvmType + "* %" + llvmVariable);
        String stepLoaded = cctx.nextRegister();
        cctx.emit(stepLoaded + " = load " + llvmType + ", " + llvmType + "* " + stepReg);
        String updatedReg = cctx.nextRegister();
        cctx.emit(updatedReg + " = add " + llvmType + " " + currForInc + ", " + stepLoaded);
        cctx.emit("store " + llvmType + " " + updatedReg + ", " + llvmType + "* %" + llvmVariable);
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

        String elemTypeLLVM = arr.getInner().getLLVMName();
        cctx.emit('%' + llvmVariable + " = alloca " + elemTypeLLVM);
        String loopVarAddr = "%" + llvmVariable;

        String loopVarValue;

        if (arr.getInner() instanceof StructType) {
            loopVarValue = loopVarAddr;
        } else {
            loopVarValue = cctx.nextRegister();
            cctx.emit(loopVarValue + " = load " + arr.getInner().getLLVMName() + ", " + toPtr(arr.getInner().getLLVMName()) + loopVarAddr);
        }

        cctx.addVariable(new RVariable(variable, true, true, arr.getInner(), loopVarAddr, loopVarValue, null));

        String startLabel = cctx.nextLabel("for_start");
        String bodyLabel = cctx.nextLabel("for_body");
        String incLabel = cctx.nextLabel("for_inc");
        String endLabel = cctx.nextLabel("for_end");

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

    private void compileStr(CompilationContext cctx, String strPtr) {
        cctx.pushScope();

        String indexReg = cctx.nextRegister();
        cctx.emit(indexReg + " = alloca i32");
        cctx.emit("store i32 0, i32* " + indexReg);

        cctx.emit("%" + llvmVariable + " = alloca i8");

        String loopVarValue = cctx.nextRegister();
        cctx.emit(loopVarValue + " = load i8, i8* %" + llvmVariable);

        cctx.addVariable(new RVariable(variable, true, true, BuiltinTypes.CHAR.getType(), "%" + llvmVariable, loopVarValue, null));

        String startLabel = cctx.nextLabel("for_start");
        String bodyLabel = cctx.nextLabel("for_body");
        String incLabel = cctx.nextLabel("for_inc");
        String endLabel = cctx.nextLabel("for_end");

        cctx.getLoopStack().push(new LoopContext(incLabel, bodyLabel, endLabel));

        cctx.emit("br label %" + startLabel);

        cctx.emit(startLabel + ":");

        String idx = cctx.nextRegister();
        cctx.emit(idx + " = load i32, i32* " + indexReg);

        String charPtr = cctx.nextRegister();
        cctx.emit(charPtr + " = getelementptr i8, i8* " + strPtr + ", i32 " + idx);

        String ch = cctx.nextRegister();
        cctx.emit(ch + " = load i8, i8* " + charPtr);

        String cond = cctx.nextRegister();
        cctx.emit(cond + " = icmp ne i8 " + ch + ", 0");
        cctx.emit("br i1 " + cond + ", label %" + bodyLabel + ", label %" + endLabel);

        cctx.emit(bodyLabel + ":");
        cctx.pushIndent();

        cctx.emit("store i8 " + ch + ", i8* %" + llvmVariable);

        block.compile(cctx);

        cctx.emit("br label %" + incLabel);
        cctx.popIndent();

        cctx.emit(incLabel + ":");

        String idx2 = cctx.nextRegister();
        cctx.emit(idx2 + " = load i32, i32* " + indexReg);

        String next = cctx.nextRegister();
        cctx.emit(next + " = add i32 " + idx2 + ", 1");

        cctx.emit("store i32 " + next + ", i32* " + indexReg);
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

    @Override
    public ForLoopNode clone() {
        return new ForLoopNode(fileName, line, variable, collection.clone(), block.clone());
    }
}
