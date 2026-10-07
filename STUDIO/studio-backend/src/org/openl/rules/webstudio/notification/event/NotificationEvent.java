package org.openl.rules.webstudio.notification.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

public class NotificationEvent extends ApplicationEvent {

    @Getter
    private final String message;

    public NotificationEvent(String message, Object source) {
        super(source);
        this.message = message;
    }
}
