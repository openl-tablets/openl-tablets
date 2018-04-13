package org.openl.message;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;

public class OpenLMessages implements IOpenLMessages {

    public static IOpenLMessages empty() {
        return new OpenLMessages();
    }

    /**
     * OpenL messages. Used to accumulate engine messages for communication with end user.
     */
    private Collection<OpenLMessage> messages = new LinkedHashSet<>();

    @Override
    public void addError(String message) {
        addMessage(message, Severity.ERROR);
    }

    /**
     * Adds new OpenL message.
     * 
     * @param message new message
     */
    @Override
    public void addMessage(OpenLMessage message) {
        messages.add(message);
    }

    @Override
    public void addMessage(String message, Severity severity) {
        OpenLMessage openlMessage = new OpenLMessage(message, severity);
        addMessage(openlMessage);
    }

    /**
     * Adds OpenL messages.
     * 
     * @param messages messages to add
     */
    @Override
    public void addMessages(Collection<OpenLMessage> messages) {
        for (OpenLMessage message : messages) {
            addMessage(message);
        }
    }

    @Override
    public void addWarning(String message) {
        addMessage(message, Severity.WARN);
    }
    
    /**
     * Removes all entries from OpenL messages.
     * 
     */
    public void clear() {
        messages = new LinkedHashSet<>();
    }

    @Override
    public Collection<OpenLMessage> getErrors() {
        return OpenLMessagesUtils.filterMessagesBySeverity(getMessages(), Severity.ERROR);
    }

    /**
     * Gets copy list of OpenL messages.
     * 
     * @return list of messages
     */
    @Override
    public Collection<OpenLMessage> getMessages() {
        return Collections.unmodifiableCollection(messages);
    }

    @Override
    public Collection<OpenLMessage> getWarnings() {
        return OpenLMessagesUtils.filterMessagesBySeverity(getMessages(), Severity.WARN);
    }

    private boolean hasBySeverity(Severity severity) {
        for (OpenLMessage message : messages) {
            if (severity.equals(message.getSeverity())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean hasErrors() {
        return hasBySeverity(Severity.ERROR);
    }

    @Override
    public boolean hasMessages() {
        return !messages.isEmpty();
    }

    @Override
    public boolean hasWarnings() {
        return hasBySeverity(Severity.WARN);
    }

}
