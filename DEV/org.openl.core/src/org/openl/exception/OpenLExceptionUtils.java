package org.openl.exception;

import java.io.PrintWriter;
import java.util.Stack;

import org.apache.commons.lang.StringUtils;
import org.openl.binding.IBoundNode;
import org.openl.main.SourceCodeURLTool;
import org.openl.source.IOpenSourceCodeModule;
import org.openl.syntax.ISyntaxNode;
import org.openl.syntax.exception.CompositeSyntaxNodeException;
import org.openl.util.text.ILocation;
import org.openl.util.text.TextInfo;

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
            SourceCodeURLTool.printCodeAndError(syntaxNode.getSourceLocation(), syntaxNode.getModule(), writer);
            SourceCodeURLTool.printSourceLocation(syntaxNode.getSourceLocation(), syntaxNode.getModule(), writer);
        }

        Stack<IBoundNode> nodes = error.getOpenlCallStack();

        for (int i = 0; i < nodes.size(); i++) {
            IBoundNode node = nodes.elementAt(i);
            SourceCodeURLTool.printSourceLocation(node.getSyntaxNode().getSourceLocation(), node.getSyntaxNode().getModule(), writer);
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

    public static String[] getErrorCode(OpenLException error) {
        ILocation location = error.getLocation();
        IOpenSourceCodeModule sourceModule = error.getSourceModule();

        String code = null;
        if (sourceModule != null) {
            code = sourceModule.getCode();
            if (StringUtils.isBlank(code)) {
                code = StringUtils.EMPTY;
            }
        }

        int pstart = 0;
        int pend = 0;

        if (StringUtils.isNotBlank(code)
                && location != null && location.isTextLocation()) {
            TextInfo info = new TextInfo(code);
            pstart = location.getStart().getAbsolutePosition(info);
            pend = Math.min(location.getEnd().getAbsolutePosition(info) + 1, code.length());
        }

        if (pend != 0) {
            return new String[] {
                    code.substring(0, pstart),
                    code.substring(pstart, pend),
                    code.substring(pend, code.length())};
        }

        return new String[0];
    }

}
