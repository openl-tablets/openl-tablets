package org.openl.message;

import org.openl.error.IOpenLError;

public class OpenLMessagesUtils {

    public static void addError(String message) {
        addMessage(message, Severity.ERROR);
    }

    public static void addError(IOpenLError error) {

        OpenLErrorMessage message = new OpenLErrorMessage(error);
        addMessage(message);
    }

    public static void addErrors(IOpenLError[] errors) {

        if (errors != null) {

            for (IOpenLError error : errors) {
                addError(error);
            }
        }
    }

    public static void addWarn(String message) {
        addMessage(message, Severity.WARN);
    }

    public static void addMessage(String message, Severity severity) {

        OpenLMessage openlMessage = new OpenLMessage(message, "", severity);
        addMessage(openlMessage);
    }

    public static void addMessage(OpenLMessage message) {
        OpenLMessages.getCurrentInstance().addMessage(message);
    }
}
