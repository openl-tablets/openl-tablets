package org.openl.studio.audit.model;

import java.time.Instant;
import java.util.Map;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import org.jspecify.annotations.Nullable;

@Schema(description = "An event of the audit log.")
@Builder
public record AuditEventView(
        @Parameter(description = "Id of the event, growing with every event recorded")
        long id,

        @Parameter(description = "When the event happened")
        Instant time,

        @Parameter(description = """
                Login name of the user who did the action. Missing for an action of OpenL Studio \
                itself, and for a failed sign-in that named no user""")
        @Nullable String actor,

        @Parameter(description = """
                How the user who did the action signed in, or tried to. Missing when it is not \
                known, such as for an action of OpenL Studio itself""")
        @Nullable AuditAuthentication authentication,

        @Parameter(description = "Address of the client the action came from")
        @Nullable String clientAddress,

        @Parameter(description = "Area the event belongs to")
        AuditCategory category,

        @Parameter(description = "What the event records")
        AuditAction action,

        @Parameter(description = "How the action ended")
        AuditOutcome outcome,

        @Parameter(description = "Kind of the object the action was done to")
        @Nullable AuditTargetType targetType,

        @Parameter(description = "Name of the object the action was done to")
        @Nullable String target,

        @Parameter(description = "Whether the subject is a user or a group")
        @Nullable AuditSubjectType subjectType,

        @Parameter(description = "User or group whose access or membership the action changed")
        @Nullable String subject,

        @Parameter(description = "What else tells the action apart, such as the values before and after a change")
        Map<String, Object> details) {
}
