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
    public void addErrorMessage(String message) {
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
    public void addWarningMessage(String message) {
        addMessage(message, Severity.WARN);
    }
    
    @Override
    public Collection<OpenLMessage> getErrorMessages() {
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
    public Collection<OpenLMessage> getWarningMessages() {
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
    public boolean hasErrorMessages() {
        return hasBySeverity(Severity.ERROR);
    }

    @Override
    public boolean hasMessages() {
        return !messages.isEmpty();
    }

    @Override
    public boolean hasWarningMessages() {
        return hasBySeverity(Severity.WARN);
    }

}
