package org.openl.syntax.code;

import java.util.Collection;
import java.util.Collections;

import org.openl.binding.IBoundCode;
import org.openl.message.OpenLMessage;
import org.openl.syntax.exception.SyntaxNodeException;

/**
 * Class that used as a container and provides information about processed code.
 */
public class ProcessedCode {

    /**
     * {@link IParsedCode} instance.
     */
    private IParsedCode parsedCode;

    /**
     * {@link IBoundCode} instance.
     */
    private IBoundCode boundCode;

    private Collection<OpenLMessage> messages;

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

    /**
     * Gets errors what was found during parsing operation.
     * 
     * @return {@link ISyntaxError}s array
     */
    public SyntaxNodeException[] getParsingErrors() {

        if (parsedCode == null) {
            return new SyntaxNodeException[0];
        }

        return parsedCode.getErrors();
    }

    /**
     * Gets errors what was found during binding operation.
     * 
     * @return {@link ISyntaxError}s array
     */
    public SyntaxNodeException[] getBindingErrors() {

        if (boundCode == null) {
            return new SyntaxNodeException[0];
        }

        return boundCode.getErrors();
    }

    public Collection<OpenLMessage> getMessages() {
        if (messages != null) {
            return Collections.unmodifiableCollection(messages);
        }
        return Collections.emptyList();
    }

    public void setMessages(Collection<OpenLMessage> messages) {
        this.messages = messages;
    }

}
