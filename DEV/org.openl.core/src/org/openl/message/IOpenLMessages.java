package org.openl.message;

import java.util.Collection;

public interface IOpenLMessages {

    void addError(String message);

    void addMessage(OpenLMessage message);

    void addMessages(Collection<OpenLMessage> messages);

    Collection<OpenLMessage> getMessages();

    boolean hasErrors();

    Collection<OpenLMessage> getErrors();
    
    boolean hasWarnings();
    
    Collection<OpenLMessage> getWarnings();
    
    boolean hasMessages();
    
    void clear();

}
