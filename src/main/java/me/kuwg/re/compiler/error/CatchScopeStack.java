package me.kuwg.re.compiler.error;

import me.kuwg.re.ast.nodes.statement.TryCatchNode;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

public final class CatchScopeStack {
    private final Deque<List<TryCatchNode.CompiledCatch>> stack = new ArrayDeque<>();

    public void pushCatchScope(List<TryCatchNode.CompiledCatch> catches) {
        stack.push(List.copyOf(catches));
    }

    public @Nullable List<TryCatchNode.CompiledCatch> popCatchScope() {
        return stack.isEmpty() ? null : stack.pop();
    }
}