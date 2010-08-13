/*
 * Created on Jul 29, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.exception;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.Stack;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.openl.binding.IBoundNode;
import org.openl.main.SourceCodeURLTool;
import org.openl.source.IOpenSourceCodeModule;
import org.openl.syntax.ISyntaxNode;
import org.openl.util.text.ILocation;

/**
 * @author snshor
 *
 */
public class OpenLRuntimeException extends RuntimeException implements OpenLException {

    private static final long serialVersionUID = -8422089115244904493L;

    private IBoundNode node;

    private Stack<IBoundNode> openlCallStack = new Stack<IBoundNode>();
    
    public OpenLRuntimeException() {
        super();
    }
    
    public OpenLRuntimeException(String message, Throwable cause) {
        super(message, cause);        
    }
    
    public OpenLRuntimeException(String message) {
        super(message);    
    }
    
    public OpenLRuntimeException(Throwable cause) {
        super(cause);    
    }

    public OpenLRuntimeException(Throwable cause, IBoundNode node) {
        super(cause);
        this.node = node;
    }
    
    public OpenLRuntimeException(String message, IBoundNode node) {
        super(message);
        this.node = node;
    }

    /* (non-Javadoc)
     * @see org.openl.exception.OpenLException#getOriginalMessage()
     */
    public String getOriginalMessage() {
        return ExceptionUtils.getRootCauseMessage(this);
    }

    /* (non-Javadoc)
     * @see org.openl.exception.OpenLException#getOriginalCause()
     */
    public Throwable getOriginalCause() {
        return ExceptionUtils.getRootCause(this);
    }

    /* (non-Javadoc)
     * @see org.openl.exception.OpenLException#getLocation()
     */
    public ILocation getLocation() {
        if (node != null) {
            ISyntaxNode syntaxNode = node.getSyntaxNode();
            return syntaxNode.getSourceLocation();
        }
        return null;
    }

    /* (non-Javadoc)
     * @see org.openl.exception.OpenLException#getSourceModule()
     */
    public IOpenSourceCodeModule getSourceModule() {
        if (node != null) {
            ISyntaxNode syntaxNode = node.getSyntaxNode();
            return syntaxNode.getModule();
        }
        return null;
    }

    public IBoundNode getNode() {
        return node;
    }

    public Stack<IBoundNode> getOpenlCallStack() {
        return openlCallStack;
    }

    @Override
    public void printStackTrace() {
        printStackTrace(System.err);
    }

    @Override
    public void printStackTrace(PrintStream s) {
        synchronized (s) {
            printStackTrace(new PrintWriter(s, true));
        }
    }

    @Override
    public void printStackTrace(PrintWriter stream) {

        Throwable rootCause = this;

        if (getCause() != null) {
            rootCause = getOriginalCause();
        }

        stream.println(rootCause.getClass().getName() + ": " + rootCause.getMessage());
        
        if (getNode() != null) {
            ISyntaxNode syntaxNode = getNode().getSyntaxNode();
            SourceCodeURLTool.printCodeAndError(syntaxNode.getSourceLocation(), syntaxNode.getModule(), stream);

            SourceCodeURLTool.printSourceLocation(syntaxNode.getSourceLocation(), syntaxNode.getModule(), stream);
        }

        Stack<IBoundNode> nodes = getOpenlCallStack();

        for (int i = 0; i < nodes.size(); i++) {
            IBoundNode node = nodes.elementAt(i);
            SourceCodeURLTool.printSourceLocation(node.getSyntaxNode().getSourceLocation(), node.getSyntaxNode()
                    .getModule(), stream);

        }

        if (rootCause != this) {
            rootCause.printStackTrace(stream);
        }
    }

    public void pushMethodNode(IBoundNode node) {
        openlCallStack.push(node);
    }

}
