package org.openl.studio.audit.service;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.AuthenticationTrustResolver;
import org.springframework.security.authentication.AuthenticationTrustResolverImpl;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import org.openl.rules.repository.api.Pageable;
import org.openl.rules.security.standalone.dao.AuditRecordDao;
import org.openl.rules.security.standalone.persistence.AuditRecord;
import org.openl.studio.audit.model.AuditEvent;
import org.openl.studio.audit.model.AuditEventView;
import org.openl.studio.audit.model.AuditEventsPage;
import org.openl.studio.audit.model.AuditQuery;

/**
 * Records the events of the audit log and reads them back.
 *
 * <p>An event is recorded in the transaction of the caller, so a change and its event are saved together: an event
 * of a change that is rolled back is rolled back too. An event recorded outside a transaction is saved at once.
 *
 * <p>In the single-user mode nothing is recorded: there is no sign-in and no access control to account for. When
 * {@code audit.outputs.console} is on, every event is also written to the console, by the {@value #LOGGER_NAME} logger.
 */
@Service
@RequiredArgsConstructor
public class AuditService {

    /** The logger every event is copied to when {@code audit.outputs.console} is on. */
    public static final String LOGGER_NAME = "OpenL.audit";

    private static final Logger AUDIT_LOGGER = LoggerFactory.getLogger(LOGGER_NAME);
    private static final ObjectMapper JSON = JsonMapper.builder().build();
    private static final AuthenticationTrustResolver TRUST_RESOLVER = new AuthenticationTrustResolverImpl();
    private static final TypeReference<Map<String, Object>> DETAILS_TYPE = new TypeReference<>() {
    };
    private static final int EXPORT_BATCH_SIZE = 500;
    /** The most events a page counts: a query that matches more is told so rather than counted to the end. */
    static final int TOTAL_LIMIT = 10_000;
    private static final String TRUNCATED = "…";
    private static final String CATEGORY = "category";
    private static final String ACTION = "action";
    private static final String OUTCOME = "outcome";
    /** The fields a line of the server log starts with or leaves out, rather than naming them. */
    private static final Set<String> LINE_START = Set.of(CATEGORY, ACTION, OUTCOME);
    private static final Map<Enum<?>, String> CODES = new ConcurrentHashMap<>();

    private final AuditRecordDao auditRecordDao;

    @Value("${user.mode}")
    private final String userMode;

    @Value("${audit.outputs.console}")
    private final boolean consoleOutput;

    /**
     * Tells whether events are recorded: everywhere but the single-user mode.
     */
    public boolean isEnabled() {
        return !"single".equals(userMode);
    }

    /**
     * Puts an event into the audit log.
     *
     * <p>The event is recorded for the signed-in user when it names no actor, and with the address of the client
     * of the current request. The target and the details are kept whole; a longer name or address than the
     * log keeps is cut.
     *
     * <p>An event published as an application event is recorded the same way, in the thread and the transaction of
     * its publisher: the services that change users, groups and tokens announce their changes so.
     */
    @EventListener
    @Transactional
    public void recordEvent(AuditEvent event) {
        append(event);
    }

    /**
     * Puts an event into the audit log unless its actor has done nothing else since an event of the same action: an
     * action repeated over and over, such as reading the log, is recorded once, when it starts.
     *
     * <p>Any other event of the actor ends the repetition, so the next one is recorded again. The events of other
     * users do not.
     */
    @Transactional
    public void recordUnlessRepeated(AuditEvent event) {
        if (!isEnabled()) {
            return;
        }
        var actor = actorOf(event);
        if (actor == null || auditRecordDao.lastActionOf(actor) != event.action()) {
            append(event);
        }
    }

    /**
     * Puts an event into the log, and copies it to the console when it is asked to.
     */
    private void append(AuditEvent event) {
        if (!isEnabled()) {
            return;
        }
        var auditRecord = new AuditRecord();
        auditRecord.setEventTime(Instant.now());
        auditRecord.setActor(actorOf(event));
        auditRecord.setAuthentication(event.authentication() != null || event.actor() != null
                ? event.authentication()
                : AuthenticationMethods.of(SecurityContextHolder.getContext().getAuthentication()));
        auditRecord.setClientAddress(cut(clientAddress(), AuditRecord.ADDRESS_LENGTH));
        auditRecord.setCategory(event.action().getCategory());
        auditRecord.setAction(event.action());
        auditRecord.setOutcome(event.outcome());
        auditRecord.setTargetType(event.targetType());
        auditRecord.setTarget(event.target());
        auditRecord.setSubjectType(event.subjectType());
        auditRecord.setSubject(cut(event.subject(), AuditRecord.NAME_LENGTH));
        auditRecord.setDetails(event.details().isEmpty() ? null : toJson(event.details()));
        auditRecordDao.save(auditRecord);
        if (consoleOutput && AUDIT_LOGGER.isInfoEnabled()) {
            copyToConsole(auditRecord);
        }
    }

    /**
     * Writes an event to the console: a line that names its fields. The fields also come as the key-value pairs
     * {@code audit.<field>}, which a structured layout, such as a JSON one, writes as fields of their own.
     */
    private static void copyToConsole(AuditRecord auditRecord) {
        var fields = fieldsOf(auditRecord);
        var line = AUDIT_LOGGER.atInfo();
        fields.forEach((name, value) -> line.addKeyValue("audit." + name, value));
        line.log(describe(fields));
    }

    /**
     * The fields of an event the server log names: the columns of an export, but the time, with the codes the REST
     * API gives. A value the event lacks is left out.
     */
    static Map<String, String> fieldsOf(AuditRecord auditRecord) {
        var fields = new LinkedHashMap<String, String>();
        putPresent(fields, "id", auditRecord.getId());
        putPresent(fields, CATEGORY, auditRecord.getCategory());
        putPresent(fields, ACTION, auditRecord.getAction());
        putPresent(fields, OUTCOME, auditRecord.getOutcome());
        putPresent(fields, "actor", auditRecord.getActor());
        putPresent(fields, "authentication", auditRecord.getAuthentication());
        putPresent(fields, "clientAddress", auditRecord.getClientAddress());
        putPresent(fields, "targetType", auditRecord.getTargetType());
        putPresent(fields, "target", auditRecord.getTarget());
        putPresent(fields, "subjectType", auditRecord.getSubjectType());
        putPresent(fields, "subject", auditRecord.getSubject());
        putPresent(fields, "details", auditRecord.getDetails());
        return fields;
    }

    private static void putPresent(Map<String, String> fields, String name, @Nullable Object value) {
        if (value != null) {
            fields.put(name, value instanceof Enum<?> constant ? codeOf(constant) : value.toString());
        }
    }

    /**
     * The line of the server log an event is copied to, such as
     * {@code signIn failure id=7 actor=jdoe clientAddress=10.0.0.7 details={"reason":"badCredentials"}}: the action
     * and the outcome, then every other field of the event but its category.
     */
    static String describe(Map<String, String> fields) {
        var line = new StringBuilder().append(fields.get(ACTION)).append(' ').append(fields.get(OUTCOME));
        fields.forEach((name, value) -> {
            if (!LINE_START.contains(name)) {
                line.append(' ').append(name).append('=');
                appendEscaped(line, value);
            }
        });
        return line.toString();
    }

    /**
     * Appends a value with its line breaks and other control characters written as escapes. A value a client typed,
     * such as the name of a refused sign-in, then cannot start a line of its own in the server log.
     */
    private static void appendEscaped(StringBuilder line, String value) {
        value.codePoints().forEach(c -> {
            switch (c) {
                case '\n' -> line.append("\\n");
                case '\r' -> line.append("\\r");
                case '\t' -> line.append("\\t");
                default -> line.append(Character.isISOControl(c) || isLineSeparator(c)
                        ? "\\u%04x".formatted(c)
                        : Character.toString(c));
            }
        });
    }

    /**
     * Whether a character is a line or a paragraph separator of Unicode, which some log readers break a line at.
     */
    private static boolean isLineSeparator(int c) {
        var type = Character.getType(c);
        return type == Character.LINE_SEPARATOR || type == Character.PARAGRAPH_SEPARATOR;
    }

    /**
     * Reads a page of the events a query matches, newest first.
     *
     * <p>The page counts the events the query matches up to {@value #TOTAL_LIMIT}, and tells whether there are more,
     * so a query that matches a large log does not read all of it. Every event a query matches is read with
     * {@link #forEach}, which never holds the whole log.
     */
    @Transactional(readOnly = true)
    public AuditEventsPage find(AuditQuery query, Pageable page) {
        var events = auditRecordDao.find(query, page.getOffset(), page.getPageSize())
                .stream()
                .map(AuditService::toView)
                .toList();
        var total = auditRecordDao.count(query, TOTAL_LIMIT + 1);
        return AuditEventsPage.page(events, page, Math.min(total, TOTAL_LIMIT), total > TOTAL_LIMIT);
    }

    /**
     * Hands every event a query matches to a consumer, newest first, reading the log a batch at a time.
     *
     * <p>Each batch is read in a short transaction of its own, so a slow consumer, such as a client downloading the
     * log, holds no database connection.
     */
    public void forEach(AuditQuery query, Consumer<AuditEventView> consumer) {
        Long lastId = null;
        List<AuditRecord> batch;
        do {
            batch = auditRecordDao.findBefore(query, lastId, EXPORT_BATCH_SIZE);
            batch.stream().map(AuditService::toView).forEach(consumer);
            if (!batch.isEmpty()) {
                lastId = batch.getLast().getId();
            }
        } while (batch.size() == EXPORT_BATCH_SIZE);
    }

    /**
     * Removes the oldest events recorded before a moment, at most the given number of them, in a transaction of its
     * own. The removal itself is recorded by the caller once it has removed them all.
     *
     * @return how many events were removed
     */
    @Transactional
    public int removeBefore(Instant time, int limit) {
        return auditRecordDao.deleteBefore(time, limit);
    }

    private static AuditEventView toView(AuditRecord auditRecord) {
        return AuditEventView.builder()
                .id(auditRecord.getId())
                .time(auditRecord.getEventTime())
                .actor(auditRecord.getActor())
                .authentication(auditRecord.getAuthentication())
                .clientAddress(auditRecord.getClientAddress())
                .category(auditRecord.getCategory())
                .action(auditRecord.getAction())
                .outcome(auditRecord.getOutcome())
                .targetType(auditRecord.getTargetType())
                .target(auditRecord.getTarget())
                .subjectType(auditRecord.getSubjectType())
                .subject(auditRecord.getSubject())
                .details(fromJson(auditRecord.getDetails()))
                .build();
    }

    /** The actor of an event as the log keeps it: the one it names, or the signed-in user. */
    private static @Nullable String actorOf(AuditEvent event) {
        return cut(Optional.ofNullable(event.actor()).orElseGet(AuditService::signedInUser), AuditRecord.NAME_LENGTH);
    }

    private static @Nullable String signedInUser() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        return TRUST_RESOLVER.isAuthenticated(authentication) ? authentication.getName() : null;
    }

    private static @Nullable String clientAddress() {
        return RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes
                ? attributes.getRequest().getRemoteAddr()
                : null;
    }

    private static @Nullable String cut(@Nullable String value, int length) {
        return value == null || value.length() <= length
                ? value
                : value.substring(0, length - TRUNCATED.length()) + TRUNCATED;
    }

    /** The code an enum value has in the REST API. */
    public static @Nullable String code(@Nullable Enum<?> value) {
        return Optional.ofNullable(value).map(AuditService::codeOf).orElse(null);
    }

    private static String codeOf(Enum<?> value) {
        return CODES.computeIfAbsent(value, constant -> JSON.convertValue(constant, String.class));
    }

    /**
     * The details of an event as the log keeps them: a JSON object.
     */
    public static String toJson(Map<String, ?> details) {
        try {
            return JSON.writeValueAsString(details);
        } catch (JsonProcessingException e) {
            return String.valueOf(details);
        }
    }

    /**
     * Reads the details back. Details that are no JSON object, as an edit of the database may leave, are handed back
     * as the text they were saved as.
     */
    private static Map<String, Object> fromJson(@Nullable String details) {
        if (details == null) {
            return Map.of();
        }
        try {
            return JSON.readValue(details, DETAILS_TYPE);
        } catch (JsonProcessingException e) {
            return Map.of("text", details);
        }
    }
}
