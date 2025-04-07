package org.openl.rules.webstudio.notification.event;

import org.springframework.context.ApplicationEvent;

public class NotificationEvent extends ApplicationEvent {

    private final String message;

    public NotificationEvent(String message, Object source) {
        super(source);
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}
