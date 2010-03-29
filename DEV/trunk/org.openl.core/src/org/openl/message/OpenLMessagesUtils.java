package org.openl.message;

import org.openl.exception.OpenLCompilationException;


public class OpenLMessagesUtils {

    public static void addError(String message) {
        addMessage(message, Severity.ERROR);
    }

    public static void addError(OpenLCompilationException error) {

        OpenLErrorMessage message = new OpenLErrorMessage(error);
        addMessage(message);
    }

    public static void addErrors(OpenLCompilationException[] errors) {

        if (errors != null) {

            for (OpenLCompilationException error : errors) {
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
