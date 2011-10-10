package org.openl.message;

import java.util.ArrayList;
import java.util.List;

public class OpenLMessages {

    /**
     * Instances of {@link OpenLMessages} per thread.
     * 
     * FIXME: not right to store messages thread local. As there can be a lot of compiled modules in one thread.
     * And in that case all the messages will be shared between modules.
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
    private List<OpenLMessage> messages = new ArrayList<OpenLMessage>();

    /**
     * Gets current instance of OpenL messages for current thread.
     * 
     * @return {@link OpenLMessages} instance
     */
    public static OpenLMessages getCurrentInstance() {
        return currentInstance.get();
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
