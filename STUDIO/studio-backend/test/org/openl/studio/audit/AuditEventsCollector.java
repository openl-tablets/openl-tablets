package org.openl.studio.audit;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.springframework.context.event.EventListener;

import org.openl.studio.audit.model.AuditAction;
import org.openl.studio.audit.model.AuditEvent;

/**
 * Collects the audit events a test context publishes, for a test to read back. Declare it as a bean of the context.
 */
public class AuditEventsCollector {

    private final List<AuditEvent> events = new CopyOnWriteArrayList<>();

    @EventListener
    public void collect(AuditEvent event) {
        events.add(event);
    }

    /** The events published so far, in order. */
    public List<AuditEvent> events() {
        return List.copyOf(events);
    }

    /** The events of one action published so far, in order. */
    public List<AuditEvent> events(AuditAction action) {
        return events.stream().filter(event -> event.action() == action).toList();
    }

    public void clear() {
        events.clear();
    }
}
