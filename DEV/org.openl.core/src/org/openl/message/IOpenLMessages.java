package org.openl.message;

import java.util.Collection;

public interface IOpenLMessages {

    void addErrorMessage(String message);
    
    void addMessage(OpenLMessage message);

    void addMessage(String message, Severity severity);

    void addMessages(Collection<OpenLMessage> messages);

    void addWarningMessage(String message);

    Collection<OpenLMessage> getErrorMessages();
    
    Collection<OpenLMessage> getMessages();
    
    Collection<OpenLMessage> getWarningMessages();
    
    boolean hasErrorMessages();
    
    boolean hasMessages();

    boolean hasWarningMessages();
}
