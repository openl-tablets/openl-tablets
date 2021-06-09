package org.openl.syntax.exception;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;

import org.openl.message.OpenLMessage;

/**
 * Handles all errors during compiling OpenL rules. Added possibility to handle list of {@link OpenLMessage}.
 */
public class CompositeOpenlException extends RuntimeException {

    /**
     * Exception message.
     */
    private final String message;
    /**
     * Syntax errors.
     */
    private final SyntaxNodeException[] errors;
    private Collection<OpenLMessage> errorMessages = new ArrayList<>();

    public CompositeOpenlException(String message,
            SyntaxNodeException[] errors,
            Collection<OpenLMessage> errorMessages) {
        super(message);
        this.message = message;
        this.errors = errors != null ? errors : SyntaxNodeException.EMPTY_ARRAY;
        if (errorMessages != null) {
            this.errorMessages = new ArrayList<>(errorMessages);
        }
    }

    private String getMessage2() {

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

    @Override
    public String getMessage() {
        String superMessage = getMessage2();
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);

        printWriter.print("+++There are " + errorMessages.size() + " exceptions\r\n");

        for (OpenLMessage message : errorMessages) {
            printWriter.print(message);
            printWriter
                .print("\r\n-------------------------------------------------------------------------------------\r\n");
        }

        printWriter.close();

        return superMessage + stringWriter.toString();
    }

    public OpenLMessage[] getErrorMessages() {
        return new ArrayList<>(errorMessages).toArray(new OpenLMessage[errorMessages.size()]);
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
