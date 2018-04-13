package org.openl.message;

import java.util.Collection;

public interface IOpenLMessages {

    void addError(String message);
    
    void addMessage(OpenLMessage message);

    void addMessage(String message, Severity severity);

    void addMessages(Collection<OpenLMessage> messages);

    void addWarning(String message);

    void clear();

    Collection<OpenLMessage> getErrors();
    
    Collection<OpenLMessage> getMessages();
    
    Collection<OpenLMessage> getWarnings();
    
    boolean hasErrors();
    
    boolean hasMessages();

    boolean hasWarnings();
}
