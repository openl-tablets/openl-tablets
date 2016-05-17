package org.openl.message;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.openl.exception.OpenLCompilationException;
import org.openl.exception.OpenLException;
import org.openl.syntax.ISyntaxNode;
import org.openl.syntax.exception.CompositeSyntaxNodeException;
import org.openl.util.CollectionUtils;
import org.openl.util.StringUtils;

public class OpenLMessagesUtils {

    public static void addError(String message) {
        addMessage(message, Severity.ERROR);
    }

    public static void addError(OpenLCompilationException error) {
        OpenLErrorMessage message = new OpenLErrorMessage(error);
        addMessage(message);
    }
    
    public static void addError(Throwable exception) {
        String errorMessage = exception.getMessage();
        
        if (StringUtils.isBlank(errorMessage)) {
            Throwable cause = exception.getCause();
            if (cause != null) {
                errorMessage = cause.getMessage();
            }
        }
        
        addError(errorMessage);
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
    
    public static void addWarn(String message, ISyntaxNode source){
        OpenLWarnMessage warn = new OpenLWarnMessage(message, source);
        addMessage(warn);
    }

    public static void addMessage(String message, Severity severity) {

        OpenLMessage openlMessage = new OpenLMessage(message, severity);
        addMessage(openlMessage);
    }

    public static void addMessage(OpenLMessage message) {
        OpenLMessages.getCurrentInstance().addMessage(message);
    }

    public static List<OpenLMessage> newMessages(OpenLException[] exceptions) {
        List<OpenLMessage> messages = new ArrayList<OpenLMessage>();

        if (CollectionUtils.isNotEmpty(exceptions)) {
            for (OpenLException error : exceptions) {
                OpenLMessage errorMessage = new OpenLErrorMessage(error);
                messages.add(errorMessage);
            }
        }

        return messages;
    }

    public static List<OpenLMessage> newMessages(Throwable exception) {
        List<OpenLMessage> messages = new ArrayList<OpenLMessage>();

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
        List<OpenLMessage> groupedMessages = groupedMessagesMap.get(severity);
        
        if (groupedMessages != null) {
            return groupedMessages;
        }
        
        return Collections.emptyList();
    }

}
