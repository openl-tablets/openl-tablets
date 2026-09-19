package org.openl.syntax.code;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.openl.binding.IBoundCode;
import org.openl.message.OpenLMessage;

/**
 * Class that used as a container and provides information about processed code.
 */
public class ProcessedCode {

    public ProcessedCode(IParsedCode parsedCode,
                         IBoundCode boundCode,
                         Collection<OpenLMessage> allMessages,
                         Collection<OpenLMessage> messages) {
        this.parsedCode = parsedCode;
        this.boundCode = boundCode;
        this.allMessages = allMessages != null ? Collections.unmodifiableCollection(allMessages)
                : List.of();
        this.messages = messages != null ? Collections.unmodifiableCollection(messages) : List.of();
    }

    /**
     * {@link IParsedCode} instance.
     */
    private IParsedCode parsedCode;

    /**
     * {@link IBoundCode} instance.
     */
    private IBoundCode boundCode;

    private final Collection<OpenLMessage> allMessages;

    private final Collection<OpenLMessage> messages;

    /**
     * Gets parsed code.
     *
     * @return {@link IParsedCode} instance
     */
    public IParsedCode getParsedCode() {
        return parsedCode;
    }

    /**
     * Gets bound code.
     *
     * @return {@link IBoundCode} instance
     */
    public IBoundCode getBoundCode() {
        return boundCode;
    }

    public Collection<OpenLMessage> getAllMessages() {
        return allMessages;
    }

    public Collection<OpenLMessage> getMessages() {
        return messages;
    }
}
