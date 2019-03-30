package org.openl.syntax.impl;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.openl.source.IOpenSourceCodeModule;
import org.openl.syntax.ISyntaxNode;
import org.openl.syntax.exception.SyntaxNodeException;
import org.openl.syntax.exception.SyntaxNodeExceptionUtils;
import org.openl.util.text.TextInterval;

/**
 * @author snshor
 */
public class SyntaxTreeBuilder implements ISyntaxConstants {

    private static final SyntaxNodeException[] SYNTAX_NODE_EXCEPTIONS = new SyntaxNodeException[0];
    private IOpenSourceCodeModule module;
    private List<SyntaxNodeException> parseErrors;
    private LinkedList<ISyntaxNode> stack = new LinkedList<>();

    public IOpenSourceCodeModule getModule() {
        return module;
    }

    public void setModule(IOpenSourceCodeModule module) {
        this.module = module;
    }

    public SyntaxNodeException[] getSyntaxErrors() {
        return parseErrors == null ? SYNTAX_NODE_EXCEPTIONS : parseErrors.toArray(SYNTAX_NODE_EXCEPTIONS);
    }

    public void addError(SyntaxNodeException exc) {
        if (parseErrors == null) {
            parseErrors = new ArrayList<>(1);
        }
        parseErrors.add(exc);
    }

    public ISyntaxNode getTopnode() {

        // TODO exception?
        switch (stack.size()) {
            case 0:
                // addError(new SyntaxException());
                return null;
            case 1:
                return pop();
            default:
                if (parseErrors != null && parseErrors.size() > 0) {
                    // it is OK to return, probably the application will check
                    // for errors
                    return pop();
                }

                // grammar problem???
                ISyntaxNode node = pop();
                addError(SyntaxNodeExceptionUtils.createError(
                    "More than one syntax node on stack:\nSource:\n" + node.getModule().getCode(),
                    null,
                    node));
                return node;
            // throw new RuntimeException("More than one syntax node on
            // stack");
        }
    }

    public void bop(String type, TextInterval pos) {
        ISyntaxNode right = pop();
        ISyntaxNode left = pop();

        push(new BinaryNode(type, pos, left, right, module));
    }

    public void emptyStatement(String type, TextInterval pos) {
        push(new EmptyNode(type, pos, module));
    }

    public void identifier(String type, TextInterval pos, String image) {
        push(new IdentifierNode(type, pos, image, module));
    }

    public void literal(String type, TextInterval pos, String image) {
        push(new LiteralNode(type, pos, image, module));
    }

    public void literal(String type, TextInterval pos, int args) {
        push(new CompositeLiteralNode(type, pos, popN(args), module));
    }

    public void nop(String type, TextInterval pos, boolean[] args) {
        int n = args.length;

        ISyntaxNode[] nodes = new ISyntaxNode[n];

        for (int i = n - 1; i >= 0; --i) {
            nodes[i] = args[i] ? pop() : null;
        }

        push(new NaryNode(type, pos, nodes, module));
    }

    public void nop(String type, TextInterval pos, int args) {
        push(new NaryNode(type, pos, popN(args), module));
    }

    public void notImplemented(String type) {
        throw new RuntimeException(type + " has not been implemented yet");
    }

    public void uop(String type, TextInterval pos) {
        ISyntaxNode left = pop();

        push(new UnaryNode(type, pos, left, module));
    }

    private void push(ISyntaxNode sn) {
        stack.push(sn);
    }

    private ISyntaxNode pop() {
        return stack.pop();
    }

    private ISyntaxNode[] popN(int n) {
        ISyntaxNode[] nodes = new ISyntaxNode[n];

        for (int i = 0; i < n; ++i) {
            nodes[n - 1 - i] = pop();
        }

        return nodes;
    }
}
