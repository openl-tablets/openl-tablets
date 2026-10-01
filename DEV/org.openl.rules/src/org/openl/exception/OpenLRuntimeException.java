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
    private final transient ILocation location;
    @Getter
    private final String sourceLocation;
    @Getter
    private final String sourceCode;

    public OpenLRuntimeException() {
        this(null, null, null);
    }

    public OpenLRuntimeException(String message, Throwable cause) {
        this(message, cause, null);
    }

    public OpenLRuntimeException(String message) {
        this(message, null, null);
    }

    public OpenLRuntimeException(Throwable cause) {
        this(cause, null);
    }

    public OpenLRuntimeException(Throwable cause, IBoundNode node) {
        this(cause == null ? null : cause.toString(), cause, syntaxNodeOf(node));
    }

    public OpenLRuntimeException(String message, IBoundNode node) {
        this(message, null, syntaxNodeOf(node));
    }

    protected OpenLRuntimeException(String message, ISyntaxNode syntaxNode) {
        this(message, null, syntaxNode);
    }

    /**
     * Remembers where in the rules the failure happened; a node without a source leaves the code unknown.
     *
     * <p>An exception created without a cause can still be given one through {@link #initCause(Throwable)}.
     */
    private OpenLRuntimeException(String message, Throwable cause, ISyntaxNode syntaxNode) {
        super(message);
        if (cause != null) {
            initCause(cause);
        }
        var module = syntaxNode == null ? null : syntaxNode.getModule();
        this.sourceCode = module == null ? null : module.getCode();
        this.location = syntaxNode == null ? null : syntaxNode.getSourceLocation();
        this.sourceLocation = syntaxNode == null ? null : SourceCodeURLTool.makeSourceLocationURL(location, module);
    }

    private static ISyntaxNode syntaxNodeOf(IBoundNode node) {
        return node == null ? null : node.getSyntaxNode();
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
                String nodeSourceLocation = SourceCodeURLTool.makeSourceLocationURL(syntaxNode.getSourceLocation(),
                        syntaxNode.getModule());
                SourceCodeURLTool.printSourceLocation(nodeSourceLocation, writer);
            }
        }

        if (rootCause != this) {
            rootCause.printStackTrace(writer);
        }
    }

}
