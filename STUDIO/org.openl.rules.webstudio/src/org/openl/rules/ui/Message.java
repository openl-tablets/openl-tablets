package org.openl.rules.ui;

import java.io.Serial;

public class Message extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    public Message(String message) {
        super(message);
    }

    public Message(String message, Throwable cause) {
        super(message, cause);
    }

}
