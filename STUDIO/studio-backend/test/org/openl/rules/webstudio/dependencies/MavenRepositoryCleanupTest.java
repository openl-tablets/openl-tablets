package org.openl.rules.webstudio.dependencies;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;

import org.openl.studio.audit.model.AuditAction;
import org.openl.studio.audit.model.AuditEvent;
import org.openl.studio.audit.service.AuditService;

class MavenRepositoryCleanupTest {

    private static final long MEGABYTE = 1024L * 1024L;

    private final MavenDependencyResolver resolver = mock(MavenDependencyResolver.class);
    private final AuditService auditService = mock(AuditService.class);

    @Test
    void removesWhatIsUnusedOrTooMuchAndRecordsIt() {
        when(resolver.cleanLocalRepository(any(), anyLong()))
                .thenReturn(new LocalMavenRepository.Removal(3, 3000));

        cleanup(30, 1024).cleanUp();

        verify(resolver).cleanLocalRepository(Duration.ofDays(30), 1024 * MEGABYTE);
        assertEquals(Map.of("trigger", "cleanup", "removed", 3, "bytes", 3000L), recorded().details());
    }

    @Test
    void recordsNothingWhenNothingIsRemoved() {
        when(resolver.cleanLocalRepository(any(), anyLong())).thenReturn(LocalMavenRepository.Removal.NONE);

        cleanup(30, 0).cleanUp();

        verify(resolver).cleanLocalRepository(Duration.ofDays(30), 0);
        verify(auditService, never()).recordEvent(any());
    }

    @Test
    void aZeroTurnsTheRuleOff() {
        when(resolver.cleanLocalRepository(any(), anyLong())).thenReturn(LocalMavenRepository.Removal.NONE);

        cleanup(0, 5).cleanUp();

        verify(resolver).cleanLocalRepository(null, 5 * MEGABYTE);
    }

    @Test
    void emptiesTheRepositoryForTheUserAndRecordsIt() {
        when(resolver.clearLocalRepository()).thenReturn(new LocalMavenRepository.Removal(5, 5000));

        cleanup(0, 0).clear();

        var event = recorded();
        assertEquals(AuditAction.MAVEN_CACHE_CLEANED, event.action());
        assertEquals(Map.of("trigger", "user", "removed", 5, "bytes", 5000L), event.details());
    }

    @Test
    void tellsTheSizeOfTheRepository() {
        when(resolver.localRepositorySize()).thenReturn(42L);

        assertEquals(42L, cleanup(30, 1024).size());
    }

    @Test
    void schedulesOnlyWhenARuleIsOn() {
        var off = cleanup(0, 0);
        off.start();
        off.stop();

        var on = cleanup(30, 0);
        on.start();
        on.stop();

        verify(resolver, never()).cleanLocalRepository(any(), anyLong());
    }

    private MavenRepositoryCleanup cleanup(int periodDays, long thresholdMegabytes) {
        return new MavenRepositoryCleanup(resolver,
                provider(auditService),
                periodDays,
                thresholdMegabytes);
    }

    private AuditEvent recorded() {
        var event = ArgumentCaptor.forClass(AuditEvent.class);
        verify(auditService).recordEvent(event.capture());
        return event.getValue();
    }

    @SuppressWarnings("unchecked")
    private static <T> ObjectProvider<T> provider(T bean) {
        var provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(bean);
        return provider;
    }
}
