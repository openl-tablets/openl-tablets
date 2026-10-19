package org.openl.studio.audit.rest.controller;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import jakarta.servlet.http.HttpServletResponse;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import org.openl.rules.repository.api.Pageable;
import org.openl.studio.audit.model.AuditAction;
import org.openl.studio.audit.model.AuditAuthentication;
import org.openl.studio.audit.model.AuditCategory;
import org.openl.studio.audit.model.AuditEvent;
import org.openl.studio.audit.model.AuditEventView;
import org.openl.studio.audit.model.AuditEventsPage;
import org.openl.studio.audit.model.AuditOutcome;
import org.openl.studio.audit.model.AuditQuery;
import org.openl.studio.audit.service.AuditService;
import org.openl.studio.common.utils.WebTool;
import org.openl.studio.rest.resolver.PaginationDefault;
import org.openl.studio.security.AdminPrivilege;

/**
 * Reads the audit log, for administrators. Every export is recorded in the log itself. Viewing the log, with nothing
 * else done in between, is recorded once, when it starts.
 */
@ConditionalOnExpression("'${user.mode}' != 'single'")
@RestController
@RequestMapping("/admin/audit")
@AdminPrivilege
@Tag(name = "Audit Log")
@RequiredArgsConstructor
public class AuditLogController {

    static final String TEXT_CSV_VALUE = "text/csv";
    private static final MediaType TEXT_CSV = MediaType.parseMediaType(TEXT_CSV_VALUE);
    private static final String FILE_NAME = "audit-log.csv";

    private final AuditService auditService;
    private final ObjectMapper objectMapper;

    @Operation(summary = "audit.get-events.summary", description = "audit.get-events.desc")
    @ApiResponse(responseCode = "406", description = "audit.get-events.406.desc")
    @ApiResponse(
            responseCode = "200",
            description = "audit.get-events.200.desc",
            headers = @Header(name = HttpHeaders.CONTENT_DISPOSITION, description = "header.content-disposition.desc"),
            content = {
                    @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = AuditEventsPage.class)),
                    @Content(mediaType = TEXT_CSV_VALUE, schema = @Schema(type = "string"))
            }
    )
    @GetMapping(produces = {MediaType.APPLICATION_JSON_VALUE, TEXT_CSV_VALUE})
    // The endpoint answers with a page of JSON or with a CSV file, depending on the Accept header, and takes
    // every criterion the log is searched by.
    @SuppressWarnings({"java:S107", "java:S1452"})
    public @Nullable ResponseEntity<?> getEvents(@RequestParam(value = "from", required = false)
                                                 @Parameter(description = "audit.param.from.desc")
                                                 @Nullable Instant from,
                                                 @RequestParam(value = "to", required = false)
                                                 @Parameter(description = "audit.param.to.desc")
                                                 @Nullable Instant to,
                                                 @RequestParam(value = "actor", required = false)
                                                 @Parameter(description = "audit.param.actor.desc")
                                                 @Nullable String actor,
                                                 @RequestParam(value = "category", required = false)
                                                 @Parameter(description = "audit.param.category.desc")
                                                 @Nullable Set<AuditCategory> categories,
                                                 @RequestParam(value = "action", required = false)
                                                 @Parameter(description = "audit.param.action.desc")
                                                 @Nullable Set<AuditAction> actions,
                                                 @RequestParam(value = "outcome", required = false)
                                                 @Parameter(description = "audit.param.outcome.desc")
                                                 @Nullable AuditOutcome outcome,
                                                 @RequestParam(value = "authentication", required = false)
                                                 @Parameter(description = "audit.param.authentication.desc")
                                                 @Nullable AuditAuthentication authentication,
                                                 @RequestParam(value = "target", required = false)
                                                 @Parameter(description = "audit.param.target.desc")
                                                 @Nullable String target,
                                                 @RequestParam(value = "subject", required = false)
                                                 @Parameter(description = "audit.param.subject.desc")
                                                 @Nullable String subject,
                                                 @RequestParam(value = "clientAddress", required = false)
                                                 @Parameter(description = "audit.param.client-address.desc")
                                                 @Nullable String clientAddress,
                                                 @PaginationDefault Pageable page,
                                                 @Parameter(description = "audit.param.accept.desc",
                                                         schema = @Schema(allowableValues = {
                                                                 MediaType.APPLICATION_JSON_VALUE, TEXT_CSV_VALUE}))
                                                 @RequestHeader(name = HttpHeaders.ACCEPT, required = false)
                                                 @Nullable String accept,
                                                 HttpServletResponse response) throws IOException {
        var query = AuditQuery.builder()
                .from(from)
                .to(to)
                .actor(actor)
                .categories(Objects.requireNonNullElse(categories, Set.of()))
                .actions(Objects.requireNonNullElse(actions, Set.of()))
                .outcome(outcome)
                .authentication(authentication)
                .target(target)
                .subject(subject)
                .clientAddress(clientAddress)
                .build();
        if (asksForCsv(accept)) {
            // An export is recorded before the file is written, so one broken off midway is still on record.
            auditService.recordEvent(readOf(query, "csv"));
            writeCsv(query, page, response);
            return null;
        }
        if (page.isUnpaged()) {
            // The whole log is written as it is read, so it is recorded first, as an export is.
            auditService.recordEvent(readOf(query, "json"));
            writeJson(query, response);
            return null;
        }
        var events = auditService.find(query, page);
        // Searching the log and turning its pages, with nothing else done in between, are one view of it, recorded
        // once. It is recorded after the page is read, so the page does not hold it.
        auditService.recordUnlessRepeated(readOf(query, "json"));
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(events);
    }

    /**
     * Writes the same events a JSON answer would hold: a page, or every event when the request is unpaged.
     *
     * <p>The events are streamed in the request thread: the servlet filters are not declared async-supported, so a
     * {@code StreamingResponseBody} would fail, and the whole log is never held in memory.
     */
    private void writeCsv(AuditQuery query, Pageable page, HttpServletResponse response) throws IOException {
        response.setContentType(TEXT_CSV_VALUE + ";charset=UTF-8");
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, WebTool.getContentDispositionValue(FILE_NAME));
        var csv = new AuditCsvWriter(new BufferedWriter(
                new OutputStreamWriter(response.getOutputStream(), StandardCharsets.UTF_8)));
        csv.writeHeader();
        if (page.isUnpaged()) {
            auditService.forEach(query, csv::write);
        } else {
            auditService.find(query, page).getContent().forEach(csv::write);
        }
        csv.flush();
    }

    /**
     * Writes every event a query matches as one page of JSON, with the same fields a page has.
     *
     * <p>The events are streamed in the request thread, as a CSV file is, so the whole log is never held in memory.
     */
    private void writeJson(AuditQuery query, HttpServletResponse response) throws IOException {
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        // The response is flushed once, when it is done, rather than after every event.
        var eventWriter = objectMapper.writer().without(SerializationFeature.FLUSH_AFTER_WRITE_VALUE);
        try (var json = objectMapper.createGenerator(response.getOutputStream())) {
            json.writeStartObject();
            json.writeArrayFieldStart("content");
            var count = new AtomicInteger();
            auditService.forEach(query, event -> {
                write(eventWriter, json, event);
                count.incrementAndGet();
            });
            json.writeEndArray();
            json.writeNumberField("pageNumber", -1);
            json.writeNumberField("pageSize", count.get());
            json.writeNumberField("numberOfElements", count.get());
            json.writeNumberField("total", count.get());
            json.writeEndObject();
        }
    }

    private static void write(ObjectWriter eventWriter, JsonGenerator json, AuditEventView event) {
        try {
            eventWriter.writeValue(json, event);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static AuditEvent readOf(AuditQuery query, String format) {
        var details = new LinkedHashMap<String, Object>();
        details.put("format", format);
        details.put("from", Optional.ofNullable(query.from()).map(Instant::toString).orElse(null));
        details.put("to", Optional.ofNullable(query.to()).map(Instant::toString).orElse(null));
        details.put("actor", query.actor());
        details.put("category", query.categories().isEmpty() ? null : codes(query.categories()));
        details.put("action", query.actions().isEmpty() ? null : codes(query.actions()));
        details.put("outcome", AuditService.code(query.outcome()));
        details.put("authentication", AuditService.code(query.authentication()));
        details.put("target", query.target());
        details.put("subject", query.subject());
        details.put("clientAddress", query.clientAddress());
        return AuditEvent.builder().action(AuditAction.AUDIT_LOG_READ).details(details).build();
    }

    private static Object codes(Set<? extends Enum<?>> values) {
        return values.stream().map(AuditService::code).sorted().toList();
    }

    /**
     * Whether the client prefers CSV to JSON: the Accept header gives CSV a higher quality than JSON. A tie, such as the
     * one of a header that names neither type or accepts any type alike, answers JSON.
     */
    private static boolean asksForCsv(@Nullable String accept) {
        if (accept == null) {
            return false;
        }
        var accepted = MediaType.parseMediaTypes(accept);
        return qualityOf(accepted, TEXT_CSV) > qualityOf(accepted, MediaType.APPLICATION_JSON);
    }

    /**
     * The quality the client gives a type: the one of the most specific range of the header that includes it, or 0
     * when none does.
     */
    private static double qualityOf(List<MediaType> accepted, MediaType type) {
        return accepted.stream()
                .filter(range -> range.includes(type))
                .min(Comparator.comparingInt(AuditLogController::wildcardsOf))
                .map(MediaType::getQualityValue)
                .orElse(0.0);
    }

    private static int wildcardsOf(MediaType range) {
        return (range.isWildcardType() ? 1 : 0) + (range.isWildcardSubtype() ? 1 : 0);
    }
}
