package org.openl.syntax.exception;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;

import org.openl.message.OpenLMessage;

/**
 * Added possibility to handle list of {@link OpenLMessage}.
 *
 */
public class CompositeOpenlException extends CompositeSyntaxNodeException {

    private static final long serialVersionUID = 5130142151601932536L;

    private Collection<OpenLMessage> errorMessages = new ArrayList<>();

    public CompositeOpenlException(String message,
            SyntaxNodeException[] errors,
            Collection<OpenLMessage> errorMessages) {
        super(message, errors);
        if (errorMessages != null) {
            this.errorMessages = new ArrayList<>(errorMessages);
        }
    }

    @Override
    public String getMessage() {
        String superMessage = super.getMessage();
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);

        printWriter.println("+++There are " + errorMessages.size() + " exceptions");

        for (OpenLMessage message : errorMessages) {
            printWriter.println(message);
            printWriter
                .println("-------------------------------------------------------------------------------------");
        }

        printWriter.close();

        return new StringBuilder().append(superMessage).append(stringWriter.toString()).toString();
    }

    public OpenLMessage[] getErrorMessages() {
        return new ArrayList<>(errorMessages).toArray(new OpenLMessage[errorMessages.size()]);
    }
}
