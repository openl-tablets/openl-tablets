/*
 * Created on Jun 9, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.syntax;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * <code>SyntaxErrorException</code> is the base exception class of those
 * exceptions that can be thrown by engine during compilation process.
 * 
 * @author snshor
 */
public class SyntaxErrorException extends RuntimeException {

    private static final long serialVersionUID = 6239517302604363701L;

    /**
     * Exception message.
     */
    private String message;

    /**
     * Syntax errors.
     */
    private ISyntaxError[] syntaxErrors;

    /**
     * Constructs new instance of the class.
     * 
     * @param message message of exception
     * @param syntaxErrors syntax errors (reason of exception)
     */
    public SyntaxErrorException(String message, ISyntaxError[] syntaxErrors) {
        super(message);

        this.message = message;
        this.syntaxErrors = syntaxErrors;
    }

    /**
     * Gets the exception message.
     * 
     * Note. Class hides the original exception message that contains error
     * stack trace and returns his own message.
     * 
     * @return exception message
     */
    @Override
    public String getMessage() {

        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);

        if (message != null) {
            printWriter.println(message);
        }

        for (int i = 0; i < syntaxErrors.length; ++i) {
            printWriter.println(syntaxErrors[i]);
        }

        printWriter.close();

        return stringWriter.toString();
    }

    /**
     * Gets syntax errors.
     * 
     * @return syntax errors
     */
    public ISyntaxError[] getSyntaxErrors() {
        return syntaxErrors;
    }
}
