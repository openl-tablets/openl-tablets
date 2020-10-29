/*
 * Created on Jun 9, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.syntax.exception;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.openl.exception.OpenlNotCheckedException;

/**
 * <code>SyntaxNodeException</code> is the base exception class of those exceptions that can be thrown by engine during
 * compilation process.
 *
 * @author snshor
 */
public class CompositeSyntaxNodeException extends OpenlNotCheckedException {

    private static final long serialVersionUID = 6239517302604363701L;

    /**
     * Exception message.
     */
    private final String message;

    /**
     * Syntax errors.
     */
    private final SyntaxNodeException[] errors;

    /**
     * Constructs new instance of the class.
     *
     * @param message message of exception
     * @param errors syntax errors (reason of exception)
     */
    public CompositeSyntaxNodeException(String message, SyntaxNodeException[] errors) {
        super(message);

        this.message = message;
        this.errors = errors;
    }

    /**
     * Gets the exception message.
     *
     * Note. Class hides the original exception message that contains error stack trace and returns his own message.
     *
     * @return exception message
     */
    @Override
    public String getMessage() {

        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);

        if (message != null) {
            printWriter.print(message);
            printWriter.print("\r\n");
        }

        if (errors != null) {
            for (SyntaxNodeException error : errors) {
                printWriter.print(error);
                printWriter.print("\r\n");
            }
        }

        printWriter.close();

        return stringWriter.toString();
    }

    /**
     * Gets syntax errors.
     *
     * @return syntax errors
     */
    public SyntaxNodeException[] getErrors() {
        return errors;
    }
}
