/*
 * Created on May 14, 2003 Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.grammar;

import java.io.Reader;
import java.util.LinkedList;

import org.openl.source.IOpenSourceCodeModule;
import org.openl.syntax.ISyntaxNode;
import org.openl.syntax.exception.SyntaxNodeException;
import org.openl.syntax.exception.SyntaxNodeExceptionUtils;
import org.openl.syntax.grammar.IGrammar;
import org.openl.syntax.impl.BinaryNode;
import org.openl.syntax.impl.CompositeLiteralNode;
import org.openl.syntax.impl.EmptyNode;
import org.openl.syntax.impl.IdentifierNode;
import org.openl.syntax.impl.LiteralNode;
import org.openl.syntax.impl.NaryNode;
import org.openl.syntax.impl.UnaryNode;
import org.openl.util.text.IPosition;
import org.openl.util.text.TextInterval;

/**
 * This class is the base for all JavaCC v3.0 and compatible grammars.
 *
 * @author snshor
 */
public abstract class JavaCC30Grammar implements IGrammar {
    protected SyntaxNodeException syntaxError;
    protected IOpenSourceCodeModule module;

    /*
     * (non-Javadoc)
     *
     * @see org.openl.syntax.IGrammar#getError()
     */
    @Override
    public SyntaxNodeException getError() {
        return syntaxError;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openl.syntax.IGrammar#setModule(org.openl.IOpenSourceCodeModule)
     */
    @Override
    public void setModule(IOpenSourceCodeModule module) {

        this.module = module;
    }

    @Override
    public void parseAsMethod(Reader r) {

        ReInit(r);
        parseTopNode("method.body");
    }

    @Override
    public void parseAsMethodHeader(Reader r) {

        ReInit(r);
        parseTopNode("method.header");
    }

    @Override
    public void parseAsModule(Reader r) {

        ReInit(r);
        parseTopNode("module");
    }

    @Override
    public void parseAsType(Reader reader) {

        ReInit(reader);
        parseTopNode("type");
    }

    protected IPosition pos(int line, int col) {

        return new JavaCC30Position(line, col);
    }

    public abstract void parseTopNode(String rootType);

    public abstract void ReInit(Reader r);

    private final LinkedList<ISyntaxNode> stack = new LinkedList<>();

    @Override
    public ISyntaxNode getTopNode() {

        // TODO exception?
        switch (stack.size()) {
            case 0:
                // addError(new SyntaxException());
                return null;
            case 1:
                return pop();
            default:
                if (syntaxError != null) {
                    // it is OK to return, probably the application will check
                    // for errors
                    return pop();
                }

                // grammar problem???
                ISyntaxNode node = pop();
                syntaxError = SyntaxNodeExceptionUtils.createError(
                        "More than one syntax node on stack:\nSource:\n" + node.getModule().getCode(),
                        null,
                        node);
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
        throw new RuntimeException(type + " is not implemented yet");
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
