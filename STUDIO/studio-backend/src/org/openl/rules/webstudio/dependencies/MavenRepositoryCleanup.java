package org.openl.rules.webstudio.dependencies;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import org.openl.studio.audit.model.AuditAction;
import org.openl.studio.audit.model.AuditEvent;
import org.openl.studio.audit.service.AuditService;

/**
 * Keeps the local Maven repository from growing without end, and empties it on demand.
 *
 * <p>A few minutes after the start, and then every hour, the cleanup removes the downloaded dependencies no project
 * has used for {@code maven.repository.cleanup.period} days, then, while the repository is larger than
 * {@code maven.repository.cleanup.threshold} MB, the ones used longest ago. A zero turns either rule off; the cleanup
 * does not run when both are off. What the projects compiled at the moment read stays.
 *
 * <p>Each instance cleans its own local repository. A cleanup that removes something, and every emptying, is recorded
 * in the audit log.
 *
 * @author Yury Molchan
 */
@Slf4j
@Component
public class MavenRepositoryCleanup {

    private static final long FIRST_RUN_DELAY_MINUTES = 5;
    private static final long RUN_INTERVAL_MINUTES = 60;
    private static final long MEGABYTE = 1024L * 1024L;

    private final MavenDependencyResolver resolver;
    private final ObjectProvider<AuditService> auditService;
    private final @Nullable Duration unusedFor;
    private final long maxBytes;
    private @Nullable ScheduledExecutorService scheduler;

    public MavenRepositoryCleanup(MavenDependencyResolver resolver,
                                  ObjectProvider<AuditService> auditService,
                                  @Value("${maven.repository.cleanup.period:0}") int periodDays,
                                  @Value("${maven.repository.cleanup.threshold:0}") long thresholdMegabytes) {
        this.resolver = resolver;
        this.auditService = auditService;
        this.unusedFor = periodDays > 0 ? Duration.ofDays(periodDays) : null;
        this.maxBytes = Math.max(thresholdMegabytes, 0) * MEGABYTE;
    }

    @PostConstruct
    void start() {
        if (unusedFor == null && maxBytes == 0) {
            return;
        }
        var executor = Executors.newSingleThreadScheduledExecutor(
                Thread.ofPlatform().daemon().name("maven-repository-cleanup").factory());
        executor.scheduleWithFixedDelay(this::cleanUpQuietly,
                FIRST_RUN_DELAY_MINUTES,
                RUN_INTERVAL_MINUTES,
                TimeUnit.MINUTES);
        scheduler = executor;
    }

    /** The size of the local Maven repository, in bytes. */
    public long size() {
        return resolver.localRepositorySize();
    }

    /** Removes the dependencies no project needs, and records what was removed. */
    void cleanUp() {
        var removal = resolver.cleanLocalRepository(unusedFor, maxBytes);
        if (removal.versions() > 0) {
            record(removal, "cleanup");
        }
    }

    /**
     * Removes every downloaded dependency, so each project downloads its dependencies again when it compiles next,
     * and records the removal for the signed-in user. A file the operating system keeps open stays.
     */
    public void clear() {
        record(resolver.clearLocalRepository(), "user");
    }

    private void record(LocalMavenRepository.Removal removal, String trigger) {
        log.info("Removed {} versions of Maven dependencies, {} bytes, from the local repository by the {}.",
                removal.versions(),
                removal.bytes(),
                trigger);
        var audit = auditService.getIfAvailable();
        if (audit != null) {
            audit.recordEvent(AuditEvent.builder()
                    .action(AuditAction.MAVEN_CACHE_CLEANED)
                    .details(Map.of("trigger", trigger, "removed", removal.versions(), "bytes", removal.bytes()))
                    .build());
        }
    }

    private void cleanUpQuietly() {
        try {
            cleanUp();
        } catch (RuntimeException e) {
            log.warn("Failed to clean the local Maven repository.", e);
        }
    }

    @PreDestroy
    void stop() {
        if (scheduler != null) {
            scheduler.shutdownNow();
        }
    }
}
