package org.openl.message;

import java.util.ArrayList;
import java.util.List;

public class OpenLMessages {

    private static ThreadLocal<OpenLMessages> currentInstance = new ThreadLocal<OpenLMessages>();

    /**
     * OpenL messages. Used to accumulate engine messages for communication with
     * end user.
     */
    private List<OpenLMessage> messages = new ArrayList<OpenLMessage>();

    public static OpenLMessages getCurrentInstance() {
        return currentInstance.get();
    }
    
    public static void setCurrentInstance(OpenLMessages messages) {
        
        if (messages == null) {
            currentInstance.remove();
        } else {
            currentInstance.set(messages);
        }
    }

    /**
     * Gets copy list of OpenL messages.
     * 
     * @return list of messages
     */
    public List<OpenLMessage> getMessages() {
        return new ArrayList<OpenLMessage>(messages);
    }

    /**
     * Removes all entries from OpenL messages.
     * 
     */
    public void clear() {
        messages = new ArrayList<OpenLMessage>();
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
    public void addMessages(List<OpenLMessage> messages) {

        for (OpenLMessage message : messages) {
            addMessage(message);
        }
    }

}
