package org.openl.message;

public class OpenLMessagesUtils {

    public static void addError(String message) {
        
        addMessage(message, Severity.ERROR);
    }

    public static void addInfo(String message) {
        
        addMessage(message, Severity.INFO);
    }

    public static void addWarn(String message) {
        
        addMessage(message, Severity.WARN);
    }

    public static void addMessage(String message, Severity severity) {
        
        OpenLMessage openlMessage = new OpenLMessage(message, "", severity);
        OpenLMessages.getCurrentInstance().addMessage(openlMessage);
    }
}
