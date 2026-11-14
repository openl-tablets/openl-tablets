package org.openl.studio.projects.service.trace;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;

import org.openl.rules.calc.element.SpreadsheetCell;
import org.openl.rules.dt.ActionInvoker;
import org.openl.rules.dt.IDecisionTable;
import org.openl.rules.method.ExecutableRulesMethod;
import org.openl.studio.projects.model.trace.FrameKind;
import org.openl.types.Invokable;
import org.openl.util.FileUtils;
import org.openl.vm.IRuntimeEnv;

/**
 * Builds a bounded trace tree while a rule runs, for a {@code trace.txt} export.
 *
 * <p>Unlike the interactive debugger this hook never suspends: it runs the rule straight to completion and
 * records each table frame, spreadsheet cell, and decision-table check with its computed value. Because the
 * legacy format prints a parent with its result before its children, each frame's sub-tree is held until it
 * returns, then rendered. The tree is capped at a node limit; once reached, further nodes are dropped and the
 * output is flagged truncated, so a huge run cannot exhaust memory.
 */
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
final class TraceExportHook implements DebugHook {

    /** One rendered trace line and its sub-lines. */
    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    private static final class Node {
        private @Nullable String text;
        private final @Nullable String uri;
        private final List<Node> children = new ArrayList<>();
        /** The table of a frame node, {@code null} for a step. */
        private final @Nullable ExecutableRulesMethod method;
        private final @Nullable FrameKind kind;
    }

    private final SourceClassifier classifier;
    private final boolean smartNumbers;
    private final int maxNodes;

    private final Node root = new Node(null, null, null);
    private final Deque<Node> open = new ArrayDeque<>();
    private final Deque<Object> frameSources = new ArrayDeque<>();
    private int nodeCount;
    private boolean truncated;

    @Override
    public <T, E extends IRuntimeEnv, R> R bracketInvoke(Invokable<? super T, E> executor, T target,
                                                         Object[] params, E env, Object source) {
        var descriptor = classifier.describeFrame(source);
        if (descriptor != null) {
            // describeFrame returns a descriptor only for an ExecutableRulesMethod, so the cast is safe.
            return frame(executor, target, params, env, (ExecutableRulesMethod) source, descriptor);
        }
        var location = classifier.describeSubStep(executor, env, frameSources.peek());
        if (location == null) {
            return executor.invoke(target, params, env);
        }
        return subStep(executor, target, params, env, location);
    }

    // A frame that fails keeps no text: its line is written with the file, as an error. The line is not built where
    // the failure surfaced: after a stack overflow no stack is left there, and a class whose initialization fails
    // there stays unusable until the JVM restarts.
    private <T, E extends IRuntimeEnv, R> R frame(Invokable<? super T, E> executor, T target, Object[] params,
                                                  E env, ExecutableRulesMethod method,
                                                  SourceClassifier.FrameDescriptor descriptor) {
        // Track the frame stack even past the node cap, so a sub-step always classifies against its true
        // enclosing table; the output node is added only while under the cap.
        var node = openNode(method.getSourceUrl(), method, descriptor.kind());
        frameSources.push(method);
        try {
            R result = executor.invoke(target, params, env);
            if (node != null) {
                node.text = TraceTextFormatter.frameLine(method, descriptor.kind(), result, smartNumbers);
            }
            return result;
        } finally {
            frameSources.pop();
            if (node != null) {
                open.pop();
            }
        }
    }

    private <T, E extends IRuntimeEnv, R> R subStep(Invokable<? super T, E> executor, T target, Object[] params,
                                                    E env, CurrentLocation location) {
        var node = openNode(frameUri(), null, null);
        if (node == null) {
            return executor.invoke(target, params, env);
        }
        try {
            R result = executor.invoke(target, params, env);
            node.text = subStepText(executor, location, result);
            return result;
        } finally {
            open.pop();
        }
    }

    private String subStepText(Object executor, CurrentLocation location, @Nullable Object value) {
        if (executor instanceof SpreadsheetCell cell) {
            String label = location.label() != null ? location.label() : cell.toString();
            return TraceTextFormatter.cellLine(label, cell.getType(), value, smartNumbers);
        }
        if (executor instanceof ActionInvoker invoker) {
            // The location label truncates a large rule list for the UI; the export prints the full list.
            return TraceTextFormatter.returnedRuleLine(resolveRuleNames(invoker.getResultRules()));
        }
        return location.label() != null ? location.label() : "";
    }

    @Override
    public void onPut(Object source, String id, Object[] args) {
        var check = classifier.describeCondition(id, args);
        if (check == null) {
            return;
        }
        addLeaf(TraceTextFormatter.conditionLine(check.conditionName(), resolveRuleNames(check.rules())));
    }

    @Override
    public boolean onResolveNode(Object executor) {
        // A re-read of an already-executed step: the value is already shown where it computed, so the export
        // does not repeat it. Reporting it as resolved stops the engine from re-running the step.
        return true;
    }

    private @Nullable Node openNode(@Nullable String uri, @Nullable ExecutableRulesMethod method,
                                    @Nullable FrameKind kind) {
        if (nodeCount >= maxNodes) {
            truncated = true;
            return null;
        }
        nodeCount++;
        var node = new Node(uri, method, kind);
        parent().children.add(node);
        open.push(node);
        return node;
    }

    /** Attach a childless line (a condition check) under the current frame without opening a scope. */
    private void addLeaf(String text) {
        if (nodeCount >= maxNodes) {
            truncated = true;
            return;
        }
        nodeCount++;
        var node = new Node(frameUri(), null, null);
        node.text = text;
        parent().children.add(node);
    }

    private Node parent() {
        var top = open.peek();
        return top != null ? top : root;
    }

    private @Nullable String frameUri() {
        var top = open.peek();
        return top != null ? top.uri : null;
    }

    /** Resolve rule indices to their names against the enclosing decision table, or their index form otherwise. */
    private List<String> resolveRuleNames(int[] rules) {
        return frameSources.peek() instanceof IDecisionTable table
                ? Arrays.stream(rules).mapToObj(table::getRuleName).toList()
                : Arrays.stream(rules).mapToObj(Integer::toString).toList();
    }

    /** Render the whole captured tree as indented {@code TRACE:} lines, flagging a node-capped run. */
    void writeTo(Writer writer) throws IOException {
        for (Node child : root.children) {
            writeNode(child, 0, writer);
        }
        if (truncated) {
            writer.write("... (trace truncated: the " + maxNodes + "-node limit was reached)\n");
        }
    }

    private void writeNode(Node node, int level, Writer writer) throws IOException {
        indent(writer, level);
        writer.write("TRACE: ");
        writer.write(lineOf(node));
        writer.write('\n');
        indent(writer, level);
        writer.write("    at ");
        writer.write(node.uri != null ? FileUtils.getBaseName(node.uri) : "");
        writer.write("&openl=\n");
        for (Node child : node.children) {
            writeNode(child, level + 1, writer);
        }
    }

    /** The line of a node. A frame without its text never returned: it failed. */
    private static String lineOf(Node node) {
        if (node.text != null) {
            return node.text;
        }
        var method = node.method;
        var kind = node.kind;
        return method != null && kind != null ? TraceTextFormatter.frameErrorLine(method, kind) : "";
    }

    private static void indent(Writer writer, int level) throws IOException {
        for (var i = 0; i < level; i++) {
            writer.write('\t');
        }
    }
}
