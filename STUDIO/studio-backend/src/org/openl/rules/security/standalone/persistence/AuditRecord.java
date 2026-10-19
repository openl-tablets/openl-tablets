package org.openl.rules.security.standalone.persistence;

import java.time.Instant;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.Setter;
import org.jspecify.annotations.Nullable;

import org.openl.studio.audit.model.AuditAction;
import org.openl.studio.audit.model.AuditAuthentication;
import org.openl.studio.audit.model.AuditCategory;
import org.openl.studio.audit.model.AuditOutcome;
import org.openl.studio.audit.model.AuditSubjectType;
import org.openl.studio.audit.model.AuditTargetType;

/**
 * An event of the audit log.
 *
 * <p>An event names its actor, target and subject by name and keeps no link to them, so it outlives the user or the
 * object it names. A value the event lacks is empty.
 */
@Entity
@Table(name = "OpenL_Audit_Log")
@Getter
@Setter
public class AuditRecord {

    /** Longest actor, subject and client address the log keeps. */
    public static final int NAME_LENGTH = 100;
    public static final int ADDRESS_LENGTH = 45;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "eventTime", nullable = false)
    private Instant eventTime;

    @Column(name = "actor", length = NAME_LENGTH)
    private @Nullable String actor;

    @Enumerated(EnumType.STRING)
    @Column(name = "authentication", length = 10)
    private @Nullable AuditAuthentication authentication;

    @Column(name = "clientAddress", length = ADDRESS_LENGTH)
    private @Nullable String clientAddress;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", length = 20, nullable = false)
    private AuditCategory category;

    @Enumerated(EnumType.STRING)
    @Column(name = "action", length = 40, nullable = false)
    private AuditAction action;

    @Enumerated(EnumType.STRING)
    @Column(name = "outcome", length = 10, nullable = false)
    private AuditOutcome outcome;

    @Enumerated(EnumType.STRING)
    @Column(name = "targetType", length = 20)
    private @Nullable AuditTargetType targetType;

    /** The object the action was done to, kept whole, however long it is. */
    @Column(name = "target")
    private @Nullable String target;

    @Enumerated(EnumType.STRING)
    @Column(name = "subjectType", length = 10)
    private @Nullable AuditSubjectType subjectType;

    @Column(name = "subject", length = NAME_LENGTH)
    private @Nullable String subject;

    /** What else tells the action apart, written as a JSON object and kept whole, however long it is. */
    @Column(name = "details")
    private @Nullable String details;
}
