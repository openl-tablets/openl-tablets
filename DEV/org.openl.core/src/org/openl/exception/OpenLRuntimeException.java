package org.openl.exception;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.LinkedList;

import org.openl.binding.IBoundNode;
import org.openl.main.SourceCodeURLTool;
import org.openl.syntax.ISyntaxNode;
import org.openl.util.text.ILocation;

/**
 * Exception, that happens at runtime time of OpenL, when rules are invoking. NOTE! Don`t use it as wrapper for java
 * runtime exceptions on compile time of OpenL.
 *
 * @author snshor
 *
 */
public class OpenLRuntimeException extends RuntimeException implements OpenLException {

    private static final long serialVersionUID = -8422089115244904493L;

    private LinkedList<IBoundNode> openlCallStack = new LinkedList<>();
    private ILocation location;
    private String sourceLocation;
    private String sourceCode;

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
        if (node != null) {
            ISyntaxNode syntaxNode = node.getSyntaxNode();
            if (syntaxNode != null) {
                this.sourceCode = syntaxNode.getModule().getCode();
                this.location = syntaxNode.getSourceLocation();
                this.sourceLocation = SourceCodeURLTool.makeSourceLocationURL(syntaxNode.getSourceLocation(),
                    syntaxNode.getModule());
            }
        }
    }

    public OpenLRuntimeException(String message, IBoundNode node) {
        super(message);
        if (node != null) {
            ISyntaxNode syntaxNode = node.getSyntaxNode();
            if (syntaxNode != null) {
                this.sourceCode = syntaxNode.getModule().getCode();
                this.location = syntaxNode.getSourceLocation();
                this.sourceLocation = SourceCodeURLTool.makeSourceLocationURL(syntaxNode.getSourceLocation(),
                    syntaxNode.getModule());
            }
        }
    }

    protected OpenLRuntimeException(String message, ISyntaxNode syntaxNode) {
        super(message);
        if (syntaxNode != null) {
            this.sourceCode = syntaxNode.getModule().getCode();
            this.location = syntaxNode.getSourceLocation();
            this.sourceLocation = SourceCodeURLTool.makeSourceLocationURL(syntaxNode.getSourceLocation(),
                syntaxNode.getModule());
        }
    }

    public String getOriginalMessage() {
        return super.getMessage();
    }

    @Override
    public String getMessage() {
        StringWriter messageWriter = new StringWriter();
        PrintWriter pw = new PrintWriter(messageWriter);
        if (location != null) {
            pw.print(super.getMessage() + "\r\n");
            SourceCodeURLTool.printCodeAndError(getLocation(), getSourceCode(), pw);
            SourceCodeURLTool.printSourceLocation(getSourceLocation(), pw);
        } else {
            pw.print(super.getMessage());
        }
        return messageWriter.toString();
    }

    @Override
    public ILocation getLocation() {
        return location;
    }

    @Override
    public String getSourceCode() {
        return sourceCode;
    }

    @Override
    public String getSourceLocation() {
        return sourceLocation;
    }

    public void pushMethodNode(IBoundNode node) {
        openlCallStack.push(node);
    }

    @Override
    public void printStackTrace() {
        printStackTrace(System.err);
    }

    @Override
    public void printStackTrace(PrintStream printStream) {
        synchronized (printStream) {
            printStackTrace(new PrintWriter(printStream, true));
        }
    }

    @Override
    public void printStackTrace(PrintWriter writer) {
        Throwable rootCause = this;

        if (getCause() != null) {
            rootCause = getCause();
        }

        writer.print(rootCause.getClass().getName() + ": " + rootCause.getMessage() + "\r\n");

        if (getLocation() != null) {
            SourceCodeURLTool.printCodeAndError(getLocation(), getSourceCode(), writer);
            SourceCodeURLTool.printSourceLocation(getSourceLocation(), writer);
        }

        LinkedList<IBoundNode> nodes = openlCallStack;

        for (IBoundNode node : nodes) {
            ISyntaxNode syntaxNode = node.getSyntaxNode();
            if (syntaxNode != null) {
                String sourceLocation = SourceCodeURLTool.makeSourceLocationURL(syntaxNode.getSourceLocation(),
                    syntaxNode.getModule());
                SourceCodeURLTool.printSourceLocation(sourceLocation, writer);
            }
        }

        if (rootCause != this) {
            rootCause.printStackTrace(writer);
        }
    }

}
