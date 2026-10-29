package org.openl.studio.audit.service;

import java.util.Map;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import org.openl.info.OpenLVersion;
import org.openl.studio.audit.model.AuditAction;
import org.openl.studio.audit.model.AuditEvent;

/**
 * Records the starts, restarts and stops of OpenL Studio in the audit log.
 *
 * <p>A start is recorded once OpenL Studio is ready to handle requests, with its version. A restart applies the
 * settings saved without a stop of the server: OpenL Studio reloads its configuration and ends the browser sessions,
 * the end of each recorded. A stop is recorded before OpenL Studio closes. A server stopped and started again
 * records a stop and a start.
 *
 * <p>OpenL Studio does these actions itself, so their events name no user. A problem with the audit log never fails
 * a start, a restart or a stop: it is logged.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ApplicationAuditor {

    static final String VERSION = "version";

    private final AuditService auditService;

    /** Records a start of OpenL Studio, with its version. */
    public void started() {
        recordQuietly(AuditEvent.builder()
                .action(AuditAction.APPLICATION_STARTED)
                .details(Map.of(VERSION, OpenLVersion.getVersion()))
                .build());
    }

    /** Records a restart of OpenL Studio that applied the settings saved. */
    public void restarted() {
        recordQuietly(AuditEvent.builder().action(AuditAction.APPLICATION_RESTARTED).build());
    }

    /** Records a stop of OpenL Studio. */
    public void stopped() {
        recordQuietly(AuditEvent.builder().action(AuditAction.APPLICATION_STOPPED).build());
    }

    private void recordQuietly(AuditEvent event) {
        try {
            auditService.recordEvent(event);
        } catch (RuntimeException e) {
            log.error("Failed to record the {} event in the audit log.", event.action(), e);
        }
    }
}
