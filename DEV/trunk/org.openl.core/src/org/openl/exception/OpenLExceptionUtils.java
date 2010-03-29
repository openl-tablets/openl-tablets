package org.openl.exception;

import java.io.PrintWriter;

import org.openl.main.SourceCodeURLTool;
import org.openl.syntax.exception.CompositeSyntaxNodeException;

public class OpenLExceptionUtils {

    private OpenLExceptionUtils() {
    }

    public static void printError(OpenLCompilationException error, PrintWriter writer) {

        Throwable cause = error.getOriginalCause();

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

        if (error.getOriginalCause() != null) {
            error.getOriginalCause().printStackTrace(writer);
        }
    }

}
