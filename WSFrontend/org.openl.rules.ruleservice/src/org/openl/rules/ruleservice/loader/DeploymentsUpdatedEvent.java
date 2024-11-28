package org.openl.rules.ruleservice.loader;

import org.springframework.context.ApplicationEvent;

public class DeploymentsUpdatedEvent extends ApplicationEvent {

    public DeploymentsUpdatedEvent(Object source) {
        super(source);
    }
}
