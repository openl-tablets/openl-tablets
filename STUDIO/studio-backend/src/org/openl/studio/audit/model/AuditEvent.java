package org.openl.studio.audit.model;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import lombok.Builder;
import org.jspecify.annotations.Nullable;

/**
 * An event to put into the audit log.
 *
 * <p>The actor is the user who did the action. An event that names no actor is recorded for the signed-in user,
 * or for no one when the action is done by OpenL Studio itself. The time and the address of the client are added
 * when the event is recorded.
 *
 * <p>The target is the object the action was done to; the subject is the user or the group whose access or
 * membership the action changed. The details hold what else tells the action apart, such as the values before
 * and after a change. They never hold a password or a token.
 *
 * @param action      what is recorded
 * @param outcome     how the action ended, {@link AuditOutcome#SUCCESS} when not given
 * @param actor       who did the action, or {@code null} for the signed-in user
 * @param authentication how the actor signed in, or tried to; {@code null} for how the signed-in user did
 * @param targetType  the kind of the object the action was done to
 * @param target      the name of the object the action was done to
 * @param subjectType whether the subject is a user or a group
 * @param subject     the name of the user or the group whose access or membership changed
 * @param details     what else tells the action apart; entries without a value are left out
 */
@Builder
public record AuditEvent(AuditAction action,
                         AuditOutcome outcome,
                         @Nullable String actor,
                         @Nullable AuditAuthentication authentication,
                         @Nullable AuditTargetType targetType,
                         @Nullable String target,
                         @Nullable AuditSubjectType subjectType,
                         @Nullable String subject,
                         Map<String, ?> details) {

    /**
     * The detail that names where a change came from.
     */
    public static final String ORIGIN = "origin";

    /**
     * The origin of a change the identity provider brought at sign-in.
     */
    public static final String IDENTITY_PROVIDER = "identityProvider";

    /**
     * The origin of a change the environment of the server brought at a start, such as the values of a Helm chart.
     */
    public static final String ENVIRONMENT = "environment";

    public AuditEvent {
        Objects.requireNonNull(action, "action");
        outcome = Objects.requireNonNullElse(outcome, AuditOutcome.SUCCESS);
        var present = new LinkedHashMap<String, Object>();
        if (details != null) {
            details.forEach((key, value) -> {
                if (value != null) {
                    present.put(key, value);
                }
            });
        }
        details = Collections.unmodifiableMap(present);
    }

    /**
     * Starts an event of a user joining or leaving a group: the group is its target and the user its subject.
     */
    public static AuditEventBuilder membership(AuditAction action, String group, String user) {
        return builder()
                .action(action)
                .targetType(AuditTargetType.GROUP)
                .target(group)
                .subjectType(AuditSubjectType.USER)
                .subject(user);
    }
}
