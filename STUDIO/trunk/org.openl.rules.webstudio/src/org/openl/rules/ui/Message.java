package org.openl.rules.ui;

public class Message extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public Message() {
    }

    public Message(String message) {
        super(message);
    }

}
