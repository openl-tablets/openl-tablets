package org.openl.message;

import java.util.*;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.openl.exception.OpenLCompilationException;
import org.openl.exception.OpenLException;
import org.openl.syntax.ISyntaxNode;
import org.openl.syntax.exception.CompositeSyntaxNodeException;
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
            Collection<OpenLMessage> messages = new ArrayList<>();
            for (OpenLCompilationException error : errors) {
                OpenLMessage message = newErrorMessage(error);
                messages.add(message);
            }
            return messages;
        }
        return Collections.emptyList();
    }

    public static OpenLMessage newWarnMessage(String message, ISyntaxNode source) {
        return new OpenLWarnMessage(message, source);
    }

    public static Collection<OpenLMessage> newMessages(OpenLException[] exceptions) {
        Collection<OpenLMessage> messages = new ArrayList<>();

        if (CollectionUtils.isNotEmpty(exceptions)) {
            for (OpenLException error : exceptions) {
                OpenLMessage errorMessage = new OpenLErrorMessage(error);
                messages.add(errorMessage);
            }
        }

        return messages;
    }

    public static OpenLMessage newErrorMessage(OpenLCompilationException error) {
        return new OpenLErrorMessage(error);
    }

    public static List<OpenLMessage> newErrorMessages(Throwable exception) {
        List<OpenLMessage> messages = new ArrayList<>();

        if (exception instanceof CompositeSyntaxNodeException) {
            CompositeSyntaxNodeException compositeException = (CompositeSyntaxNodeException) exception;
            OpenLException[] exceptions = compositeException.getErrors();

            for (OpenLException openLException : exceptions) {
                OpenLMessage errorMessage = new OpenLErrorMessage(openLException);
                messages.add(errorMessage);
            }

        } else if (exception instanceof OpenLException) {
            OpenLException openLException = (OpenLException) exception;
            OpenLMessage errorMessage = new OpenLErrorMessage(openLException);
            messages.add(errorMessage);
        } else {
            OpenLMessage message = new OpenLMessage(ExceptionUtils.getRootCauseMessage(exception), Severity.ERROR);
            messages.add(message);
        }

        return messages;
    }

    private static Map<Severity, Collection<OpenLMessage>> groupMessagesBySeverity(Collection<OpenLMessage> messages) {
        Map<Severity, Collection<OpenLMessage>> groupedMessagesMap = new HashMap<>();

        for (OpenLMessage message : messages) {
            Severity severity = message.getSeverity();
            Collection<OpenLMessage> groupedMessages = groupedMessagesMap.get(severity);

            if (groupedMessages == null) {
                groupedMessages = new ArrayList<>();
                groupedMessagesMap.put(severity, groupedMessages);
            }

            groupedMessages.add(message);
        }

        return groupedMessagesMap;
    }

    public static Collection<OpenLMessage> filterMessagesBySeverity(Collection<OpenLMessage> messages,
            Severity severity) {
        Map<Severity, Collection<OpenLMessage>> groupedMessagesMap = groupMessagesBySeverity(messages);
        Collection<OpenLMessage> groupedMessages = groupedMessagesMap.get(severity);

        if (groupedMessages != null) {
            return groupedMessages;
        }

        return Collections.emptyList();
    }
}
