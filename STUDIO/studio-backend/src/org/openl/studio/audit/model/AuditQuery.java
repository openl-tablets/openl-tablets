package org.openl.studio.audit.model;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;

import lombok.Builder;
import org.jspecify.annotations.Nullable;

/**
 * Which events of the audit log to read. A criterion left out matches every event.
 *
 * <p>The period includes its start and excludes its end. The actor, the target and the subject match an event
 * whose value contains the given text, in any case.
 *
 * @param from       the first moment of the period
 * @param to         the moment the period ends
 * @param actor      text the actor contains
 * @param categories the categories of the events; empty for all of them
 * @param actions    the actions of the events; empty for all of them
 * @param outcome    how the actions ended
 * @param authentication how the users who did the actions signed in
 * @param target     text the target contains
 * @param subject    text the subject contains
 */
@Builder
public record AuditQuery(@Nullable Instant from,
                         @Nullable Instant to,
                         @Nullable String actor,
                         Set<AuditCategory> categories,
                         Set<AuditAction> actions,
                         @Nullable AuditOutcome outcome,
                         @Nullable AuditAuthentication authentication,
                         @Nullable String target,
                         @Nullable String subject,
                         @Nullable String clientAddress) {

    public AuditQuery {
        categories = Optional.ofNullable(categories).map(Set::copyOf).orElseGet(Set::of);
        actions = Optional.ofNullable(actions).map(Set::copyOf).orElseGet(Set::of);
    }
}
