package org.openl.rules.ui.repository.handlers;

import java.util.LinkedList;
import java.util.List;

public class MessageQueue {
    private LinkedList<Exception> messages;

    public MessageQueue() {
        messages = new LinkedList<Exception>();
    }

    public void addMessage(Exception e) {
        messages.add(e);
    }

    public List<Error> getAll() {
        List<Error> result = (List<Error>) messages.clone();
        return result;
    }

    public void clear() {
        messages.clear();
    }
}
