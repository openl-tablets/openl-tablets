package org.openl.message;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;

public class OpenLMessages implements IOpenLMessages {

    public static IOpenLMessages empty() {
        return new OpenLMessages();
    }

    /**
     * Instances of {@link OpenLMessages} per thread.
     * 
     * FIXME: not right to store messages thread local. As there can be a lot of compiled modules in one thread. And in
     * that case all the messages will be shared between modules.
     * 
     * @author DLiauchuk
     */
    private static ThreadLocal<OpenLMessages> currentInstance = new ThreadLocal<OpenLMessages>() {
        @Override
        protected OpenLMessages initialValue() {
            return new OpenLMessages();
        }
    };

    /**
     * OpenL messages. Used to accumulate engine messages for communication with end user.
     */
    private Collection<OpenLMessage> messages = new LinkedHashSet<>();

    /**
     * Gets current instance of OpenL messages for current thread.
     * 
     * @return {@link OpenLMessages} instance
     */
    public static OpenLMessages getCurrentInstance() {
        return currentInstance.get();
    }

    /**
     * Removes current instance of OpenL messages for current thread.
     */
    public static void removeCurrentInstance() {
        currentInstance.remove();
    }

    /**
     * Gets copy list of OpenL messages.
     * 
     * @return list of messages
     */
    public Collection<OpenLMessage> getMessages() {
        return Collections.unmodifiableCollection(messages);
    }

    /**
     * Removes all entries from OpenL messages.
     * 
     */
    public void clear() {
        messages = new LinkedHashSet<>();
    }

    /**
     * Adds new OpenL message.
     * 
     * @param message new message
     */
    public void addMessage(OpenLMessage message) {
        messages.add(message);
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
    public void addError(String message) {
        addMessage(message, Severity.ERROR);
    }

    public void addMessage(String message, Severity severity) {
        OpenLMessage openlMessage = new OpenLMessage(message, severity);
        addMessage(openlMessage);
    }

    @Override
    public Collection<OpenLMessage> getErrors() {
        return OpenLMessagesUtils.filterMessagesBySeverity(getMessages(), Severity.ERROR);
    }

    @Override
    public Collection<OpenLMessage> getWarnings() {
        return OpenLMessagesUtils.filterMessagesBySeverity(getMessages(), Severity.WARN);
    }

    @Override
    public boolean hasErrors() {
        return hasBySeverity(Severity.ERROR);
    }

    @Override
    public boolean hasWarnings() {
        return hasBySeverity(Severity.WARN);
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
    public boolean hasMessages() {
        return !messages.isEmpty();
    }

}
