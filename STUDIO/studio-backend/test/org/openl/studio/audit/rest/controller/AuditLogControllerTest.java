package org.openl.studio.audit.rest.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletResponse;

import org.openl.rules.repository.api.Offset;
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

class AuditLogControllerTest {

    private static final AuditEventView SIGN_IN = AuditEventView.builder()
            .id(1)
            .time(Instant.parse("2026-10-19T10:15:30Z"))
            .actor("jdoe")
            .authentication(AuditAuthentication.FORM)
            .category(AuditCategory.AUTHENTICATION)
            .action(AuditAction.SIGN_IN)
            .outcome(AuditOutcome.SUCCESS)
            .details(Map.of())
            .build();

    private static final ObjectMapper JSON = new ObjectMapper().registerModule(new JavaTimeModule())
            .setDefaultPropertyInclusion(JsonInclude.Include.NON_EMPTY)
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private AuditService auditService;
    private AuditLogController controller;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        auditService = mock(AuditService.class);
        controller = new AuditLogController(auditService, JSON);
        response = new MockHttpServletResponse();
    }

    @Test
    void answersAPageOfJsonAndRecordsTheRead() throws IOException {
        var page = AuditEventsPage.page(List.of(SIGN_IN), Offset.of(0, 50), 1, false);
        when(auditService.find(any(), any())).thenReturn(page);
        var from = Instant.parse("2026-10-01T00:00:00Z");

        var answer = controller.getEvents(from, null, "jdoe", Set.of(AuditCategory.AUTHENTICATION),
                Set.of(AuditAction.SIGN_IN, AuditAction.SIGN_OUT), AuditOutcome.FAILURE, AuditAuthentication.TOKEN,
                null, null, "10.0.0", Offset.of(0, 50), "application/json", response);

        assertSame(page, answer.getBody());
        var query = ArgumentCaptor.forClass(AuditQuery.class);
        verify(auditService).find(query.capture(), eq(Offset.of(0, 50)));
        assertEquals(from, query.getValue().from());
        assertEquals("jdoe", query.getValue().actor());
        assertEquals(Set.of(AuditCategory.AUTHENTICATION), query.getValue().categories());
        assertEquals(AuditAuthentication.TOKEN, query.getValue().authentication());
        assertEquals("10.0.0", query.getValue().clientAddress());
        assertEquals(Map.of("format", "json",
                "from", "2026-10-01T00:00:00Z",
                "actor", "jdoe",
                "category", List.of("authentication"),
                "action", List.of("signIn", "signOut"),
                "outcome", "failure",
                "authentication", "token",
                "clientAddress", "10.0.0"), recordedView().details());
    }

    @Test
    void recordsEveryPageAsAViewUnlessItRepeatsOne() throws IOException {
        when(auditService.find(any(), any())).thenReturn(AuditEventsPage.page(List.of(), Offset.of(50, 50), 0, false));

        controller.getEvents(null, null, "jdoe", null, null, null, null, null, null, null, Offset.of(50, 50),
                "application/json", response);

        assertEquals(Map.of("format", "json", "actor", "jdoe"), recordedView().details());
        verify(auditService, never()).recordEvent(any());
    }

    @Test
    void answersJsonWhenTheClientAsksForAnything() throws IOException {
        var page = Offset.of(0, 50);
        when(auditService.find(any(), any())).thenReturn(AuditEventsPage.page(List.of(), page, 0, false));

        controller.getEvents(null, null, null, null, null, null, null, null, null, null, page, null, response);
        controller.getEvents(null, null, null, null, null, null, null, null, null, null, page, "*/*", response);

        assertEquals(Map.of("format", "json"), recordedView().details());
    }

    @Test
    void streamsEveryEventAsTheOnePageAfterRecordingTheRead() throws IOException {
        doAnswer(invocation -> {
            Consumer<AuditEventView> consumer = invocation.getArgument(1);
            consumer.accept(SIGN_IN);
            consumer.accept(SIGN_IN);
            return null;
        }).when(auditService).forEach(any(), any());

        var answer = controller.getEvents(null, null, null, null, null, null, null, null, null, null,
                Pageable.unpaged(),
                "application/json", response);

        assertNull(answer, "The events are written into the response");
        assertEquals("application/json;charset=UTF-8", response.getContentType());
        var whole = AuditEventsPage.page(List.of(SIGN_IN, SIGN_IN), Pageable.unpaged(), 2, false);
        assertEquals(JSON.readTree(JSON.writeValueAsBytes(whole)), JSON.readTree(response.getContentAsByteArray()),
                "The same fields as a page holding every event");
        var order = inOrder(auditService);
        order.verify(auditService).recordEvent(any());
        order.verify(auditService).forEach(any(), any());
        verify(auditService, never()).find(any(), any());
        assertEquals(Map.of("format", "json"), recordedRead().details());
    }

    @Test
    void exportsCsvAfterRecordingTheExport() throws IOException {
        doAnswer(invocation -> {
            Consumer<AuditEventView> consumer = invocation.getArgument(1);
            consumer.accept(SIGN_IN);
            return null;
        }).when(auditService).forEach(any(), any());

        var answer = controller.getEvents(null, null, null, null, null, null, null, "Sales", "Analysts", null,
                Pageable.unpaged(), "text/csv, application/json;q=0.9", response);

        assertNull(answer, "The file is written into the response");
        assertEquals("text/csv;charset=UTF-8", response.getContentType());
        assertTrue(response.getHeader(HttpHeaders.CONTENT_DISPOSITION).contains("audit-log.csv"));
        var lines = response.getContentAsString().split("\r\n");
        assertEquals(2, lines.length);
        assertEquals("1,2026-10-19T10:15:30Z,jdoe,form,,authentication,signIn,success,,,,,", lines[1]);
        var order = inOrder(auditService);
        order.verify(auditService).recordEvent(any());
        order.verify(auditService).forEach(any(), any());
        assertEquals(Map.of("format", "csv", "target", "Sales", "subject", "Analysts"), recordedRead().details());
    }

    @Test
    void exportsThePageAJsonAnswerWouldHold() throws IOException {
        var page = Offset.of(0, 1);
        when(auditService.find(any(), eq(page))).thenReturn(AuditEventsPage.page(List.of(SIGN_IN), page, 5, false));

        controller.getEvents(null, null, null, null, null, null, null, null, null, null, page, "text/csv", response);

        var lines = response.getContentAsString().split("\r\n");
        assertEquals(2, lines.length, "The header and the one event of the page");
        verify(auditService, never()).forEach(any(), any());
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "application/json, text/csv;q=0.1 | false",
            "text/csv;q=0.5, */*             | false",
            "*/*                             | false",
            "text/*                          | true",
            "application/*;q=0.2, text/csv   | true"})
    void answersTheFormTheClientPrefers(String accept, boolean csv) throws IOException {
        var page = Offset.of(0, 1);
        when(auditService.find(any(), eq(page))).thenReturn(AuditEventsPage.page(List.of(SIGN_IN), page, 5, false));

        var answer = controller.getEvents(null, null, null, null, null, null, null, null, null, null, page, accept,
                response);

        assertEquals(csv, answer == null, "CSV is written into the response, JSON is answered");
        assertEquals(csv ? "csv" : "json", (csv ? recordedRead() : recordedView()).details().get("format"));
    }

    /** The read the log records every time: an export or an unpaged read. */
    private AuditEvent recordedRead() {
        var event = ArgumentCaptor.forClass(AuditEvent.class);
        verify(auditService, atLeastOnce()).recordEvent(event.capture());
        return readOf(event);
    }

    /** The view of a page the log records unless it repeats one. */
    private AuditEvent recordedView() {
        var event = ArgumentCaptor.forClass(AuditEvent.class);
        verify(auditService, atLeastOnce()).recordUnlessRepeated(event.capture());
        return readOf(event);
    }

    private static AuditEvent readOf(ArgumentCaptor<AuditEvent> event) {
        var read = event.getValue();
        assertEquals(AuditAction.AUDIT_LOG_READ, read.action());
        return read;
    }
}
