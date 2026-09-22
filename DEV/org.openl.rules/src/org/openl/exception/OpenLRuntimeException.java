package org.openl.exception;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.Serial;
import java.io.StringWriter;
import java.util.LinkedList;

import lombok.Getter;

import org.openl.binding.IBoundNode;
import org.openl.main.SourceCodeURLTool;
import org.openl.syntax.ISyntaxNode;
import org.openl.util.text.ILocation;

/**
 * Exception, that happens at runtime time of OpenL, when rules are invoking. NOTE! Don`t use it as wrapper for java
 * runtime exceptions on compile time of OpenL.
 *
 * @author snshor
 */
public class OpenLRuntimeException extends RuntimeException implements OpenLException {

    @Serial
    private static final long serialVersionUID = -8422089115244904493L;

    private final LinkedList<IBoundNode> openlCallStack = new LinkedList<>();
    @Getter
    private transient ILocation location;
    @Getter
    private String sourceLocation;
    @Getter
    private String sourceCode;

    public OpenLRuntimeException() {
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
                pointTo(syntaxNode);
            }
        }
    }

    public OpenLRuntimeException(String message, IBoundNode node) {
        super(message);
        if (node != null) {
            ISyntaxNode syntaxNode = node.getSyntaxNode();
            if (syntaxNode != null) {
                pointTo(syntaxNode);
            }
        }
    }

    protected OpenLRuntimeException(String message, ISyntaxNode syntaxNode) {
        super(message);
        if (syntaxNode != null) {
            pointTo(syntaxNode);
        }
    }

    /** Remembers where in the rules the failure happened; a node without a source leaves the code unknown. */
    private void pointTo(ISyntaxNode syntaxNode) {
        var module = syntaxNode.getModule();
        this.sourceCode = module == null ? null : module.getCode();
        this.location = syntaxNode.getSourceLocation();
        this.sourceLocation = SourceCodeURLTool.makeSourceLocationURL(location, module);
    }

    public String getOriginalMessage() {
        return super.getMessage();
    }

    @Override
    public String getMessage() {
        if (super.getMessage() == null) {
            return null;
        }
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

    public void pushMethodNode(IBoundNode node) {
        openlCallStack.push(node);
    }

    @Override
    public void printStackTrace() {
        printStackTrace(System.err);
    }

    @Override
    public void printStackTrace(PrintStream printStream) {
        var trace = new StringWriter();
        var writer = new PrintWriter(trace);
        printStackTrace(writer);
        writer.flush();
        // The whole trace leaves in a single write, so a trace printed in parallel cannot be mixed into it.
        printStream.print(trace.toString());
        printStream.flush();
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

        for (IBoundNode node : openlCallStack) {
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
