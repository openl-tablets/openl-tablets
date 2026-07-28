package org.openl.message;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.exception.ExceptionUtils;

import org.openl.exception.OpenLCompilationException;
import org.openl.exception.OpenLException;
import org.openl.syntax.ISyntaxNode;
import org.openl.util.CollectionUtils;

public class OpenLMessagesUtils {

    private OpenLMessagesUtils() {
    }

    public static OpenLMessage newErrorMessage(String summary) {
        return new OpenLMessage(summary, Severity.ERROR);
    }

    public static OpenLMessage newWarnMessage(String summary) {
        return new OpenLMessage(summary, Severity.WARN);
    }

    public static Collection<OpenLMessage> newErrorMessages(OpenLCompilationException[] errors) {
        if (errors != null) {
            var messages = new ArrayList<OpenLMessage>();
            for (OpenLCompilationException error : errors) {
                OpenLMessage message = newErrorMessage(error);
                messages.add(message);
            }
            return messages;
        }
        return List.of();
    }

    public static OpenLMessage newWarnMessage(String message, ISyntaxNode source) {
        return new OpenLWarnMessage(message, source);
    }

    public static Collection<OpenLMessage> newMessages(OpenLException[] exceptions) {
        var messages = new ArrayList<OpenLMessage>();

        if (CollectionUtils.isNotEmpty(exceptions)) {
            for (OpenLException error : exceptions) {
                var errorMessage = new OpenLErrorMessage(error);
                messages.add(errorMessage);
            }
        }

        return messages;
    }

    public static OpenLMessage newErrorMessage(OpenLCompilationException error) {
        return new OpenLErrorMessage(error);
    }

    public static List<OpenLMessage> newErrorMessages(Throwable exception) {
        var messages = new ArrayList<OpenLMessage>();

        if (exception instanceof OpenLException openLException) {
            var errorMessage = new OpenLErrorMessage(openLException);
            messages.add(errorMessage);
        } else {
            var message = new OpenLMessage(ExceptionUtils.getRootCauseMessage(exception), Severity.ERROR);
            messages.add(message);
        }

        return messages;
    }

    private static Map<Severity, Collection<OpenLMessage>> groupMessagesBySeverity(Collection<OpenLMessage> messages) {
        var groupedMessagesMap = new HashMap<Severity, Collection<OpenLMessage>>();

        for (OpenLMessage message : messages) {
            var severity = message.getSeverity();
            var groupedMessages = groupedMessagesMap.computeIfAbsent(severity,
                    k -> new ArrayList<>());
            groupedMessages.add(message);
        }

        return groupedMessagesMap;
    }

    public static Collection<OpenLMessage> filterMessagesBySeverity(Collection<OpenLMessage> messages,
                                                                    Severity severity) {
        var groupedMessagesMap = groupMessagesBySeverity(messages);
        Collection<OpenLMessage> groupedMessages = groupedMessagesMap.get(severity);

        if (groupedMessages != null) {
            return groupedMessages;
        }

        return List.of();
    }
}
