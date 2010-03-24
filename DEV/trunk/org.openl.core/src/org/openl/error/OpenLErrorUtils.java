package org.openl.error;

import java.io.PrintWriter;

import org.openl.main.SourceCodeURLTool;
import org.openl.syntax.exception.SyntaxNodeException;

public abstract class OpenLErrorUtils {

    public static void printError(IOpenLError error, PrintWriter writer) {

        Throwable cause = error.getOriginalCause();

        String message;

        if (cause != null && cause instanceof SyntaxNodeException) {

            SyntaxNodeException syntaxErrorException = (SyntaxNodeException) cause;

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
