package org.openl.message;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.ArrayUtils;
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

    public static List<OpenLMessage> newMessages(OpenLCompilationException[] errors) {
        List<OpenLMessage> messages = new ArrayList<OpenLMessage>();

        if (ArrayUtils.isNotEmpty(errors)) {
            for (OpenLCompilationException error : errors) {
                OpenLMessage message = new OpenLErrorMessage(error);
                messages.add(message);
            }
        }

        return messages;
    }

    public static Map<Severity, List<OpenLMessage>> groupMessagesBySeverity(List<OpenLMessage> messages) {
        Map<Severity, List<OpenLMessage>> groupedMessagesMap = new HashMap<Severity, List<OpenLMessage>>();

        for (OpenLMessage message : messages) {
            Severity severity = message.getSeverity();
            List<OpenLMessage> groupedMessages = groupedMessagesMap.get(severity);
            if (groupedMessages == null) {
                groupedMessages = new ArrayList<OpenLMessage>();
                groupedMessagesMap.put(severity, groupedMessages);
            }
            groupedMessages.add(message);
        }

        return groupedMessagesMap;
    }

    public static List<OpenLMessage> filterMessagesBySeverity(List<OpenLMessage> messages, Severity severity) {
        Map<Severity, List<OpenLMessage>> groupedMessagesMap = groupMessagesBySeverity(messages);
        return groupedMessagesMap.get(severity);
    }

}
