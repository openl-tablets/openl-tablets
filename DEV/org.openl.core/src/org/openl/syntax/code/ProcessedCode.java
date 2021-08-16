package org.openl.syntax.code;

import java.util.Collection;
import java.util.Collections;

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
                                               : Collections.emptyList();
        this.messages = messages != null ? Collections.unmodifiableCollection(messages) : Collections.emptyList();
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
     * Sets parsed code.
     *
     * @param parsedCode {@link IParsedCode} instance
     */
    public void setParsedCode(IParsedCode parsedCode) {
        this.parsedCode = parsedCode;
    }

    /**
     * Gets bound code.
     *
     * @return {@link IBoundCode} instance
     */
    public IBoundCode getBoundCode() {
        return boundCode;
    }

    /**
     * Sets bound code.
     *
     * @return {@link IBoundCode} instance
     */
    public void setBoundCode(IBoundCode boundCode) {
        this.boundCode = boundCode;
    }

    public Collection<OpenLMessage> getAllMessages() {
        return allMessages;
    }

    public Collection<OpenLMessage> getMessages() {
        return messages;
    }
}
