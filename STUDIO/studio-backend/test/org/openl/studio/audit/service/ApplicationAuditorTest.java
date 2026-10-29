package org.openl.studio.audit.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import org.openl.info.OpenLVersion;
import org.openl.studio.audit.model.AuditAction;
import org.openl.studio.audit.model.AuditEvent;

class ApplicationAuditorTest {

    private final AuditService auditService = mock(AuditService.class);
    private final ApplicationAuditor auditor = new ApplicationAuditor(auditService);

    @Test
    void recordsAStartWithTheVersionThenARestartAndAStop() {
        auditor.started();
        auditor.restarted();
        auditor.stopped();

        var events = recorded(3);
        assertEquals(List.of(AuditAction.APPLICATION_STARTED, AuditAction.APPLICATION_RESTARTED,
                AuditAction.APPLICATION_STOPPED), events.stream().map(AuditEvent::action).toList());
        assertEquals(Map.of(ApplicationAuditor.VERSION, OpenLVersion.getVersion()), events.getFirst().details());
        events.forEach(event -> assertNull(event.actor(), "OpenL Studio acts itself"));
        assertEquals(Map.of(), events.getLast().details());
    }

    @Test
    void neverFailsAStopForTheAuditLog() {
        doThrow(new IllegalStateException("database is gone")).when(auditService).recordEvent(any());

        assertDoesNotThrow(auditor::stopped);
    }

    private List<AuditEvent> recorded(int count) {
        var event = ArgumentCaptor.forClass(AuditEvent.class);
        verify(auditService, times(count)).recordEvent(event.capture());
        return event.getAllValues();
    }
}
