package org.openl.exception;

import java.io.PrintWriter;
import java.util.Stack;

import org.openl.binding.IBoundNode;
import org.openl.main.SourceCodeURLTool;
import org.openl.syntax.ISyntaxNode;
import org.openl.syntax.exception.CompositeSyntaxNodeException;

public class OpenLExceptionUtils {

    private OpenLExceptionUtils() {
    }
    
    public static OpenLException getOpenLRootCause(Throwable exception) {
        
        if (exception == null) {
            return null;
        }
        
        OpenLException cause = null;
        
            while ((exception = exception.getCause()) != null) {
                if (exception instanceof OpenLException) {
                    cause = (OpenLException) exception;
                }
            }
        
        return cause;
    }
    
    public static String getOpenLExceptionMessage(OpenLException ex) {
        
        if ( ex == null || !(ex instanceof Throwable)) {
            return null;
        }
        
        Throwable t = (Throwable) ex;
        OpenLException cause = getOpenLRootCause(t);
        
        if (cause != null) {
            return cause.getMessage();
        }
        
        return ex.getMessage();
    }
    
    public static void printRuntimeError(OpenLRuntimeException error, PrintWriter writer) {
        
        if (error == null) {
            return;
        }
        
        Throwable rootCause = error;

        if (error.getCause() != null) {
            rootCause = error.getCause();
        }

        writer.println(rootCause.getClass().getName() + ": " + rootCause.getMessage());
        
        if (error.getNode() != null) {
            ISyntaxNode syntaxNode = error.getNode().getSyntaxNode();
            if (syntaxNode != null) {
                SourceCodeURLTool.printCodeAndError(syntaxNode.getSourceLocation(), syntaxNode.getModule(), writer);
                SourceCodeURLTool.printSourceLocation(syntaxNode.getSourceLocation(), syntaxNode.getModule(), writer);
            }
        }

        Stack<IBoundNode> nodes = error.getOpenlCallStack();

        for (int i = 0; i < nodes.size(); i++) {
            IBoundNode node = nodes.elementAt(i);
            ISyntaxNode syntaxNode = node.getSyntaxNode();
            if (syntaxNode != null) {
                SourceCodeURLTool.printSourceLocation(syntaxNode.getSourceLocation(), syntaxNode.getModule(), writer);
            }
        }

        if (rootCause != error) {
            rootCause.printStackTrace(writer);
        }
    }

    public static void printError(OpenLException error, PrintWriter writer) {

        Throwable cause = error.getCause();

        String message;

        if (cause != null && cause instanceof CompositeSyntaxNodeException) {

            CompositeSyntaxNodeException syntaxErrorException = (CompositeSyntaxNodeException) cause;

            for (int i = 0; i < syntaxErrorException.getErrors().length; i++) {
                printError(syntaxErrorException.getErrors()[i], writer);
            }

            return;

        } else {
            message = error.getMessage();
        }

        writer.println("Error: " + message);

        SourceCodeURLTool.printCodeAndError(error.getLocation(), error.getSourceModule(), writer);
        SourceCodeURLTool.printSourceLocation(error, writer);

        if (error.getCause() != null) {
            error.getCause().printStackTrace(writer);
        }
    }
}
