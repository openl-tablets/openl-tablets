package org.openl.studio.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.UncheckedIOException;
import java.nio.file.FileSystemException;
import java.nio.file.InvalidPathException;
import java.util.ArrayList;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.TypeMismatchException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;

import org.openl.rules.lang.xls.WorkbookChangedOutsideException;
import org.openl.studio.audit.model.AuditAction;
import org.openl.studio.audit.model.AuditEvent;
import org.openl.studio.audit.model.AuditOutcome;
import org.openl.studio.audit.model.AuditTargetType;
import org.openl.studio.common.exception.ForbiddenException;
import org.openl.studio.common.exception.NotFoundException;
import org.openl.studio.common.model.BaseError;
import org.openl.util.FileLockedException;

class ApiExceptionControllerAdviceTest {

    @Test
    void forbiddenRequest_isRecordedInTheAuditLog() {
        var events = new ArrayList<Object>();
        var httpRequest = new MockHttpServletRequest("DELETE", "/studio/rest/users/jdoe");
        httpRequest.setContextPath("/studio");

        var response = advice(events::add).handleAllRestRuntimeExceptions(
                new ForbiddenException("users.cannot-delete-yourself.message"), new ServletWebRequest(httpRequest));

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        var event = assertInstanceOf(AuditEvent.class, events.getFirst());
        assertEquals(AuditAction.ACCESS_DENIED, event.action());
        assertEquals(AuditOutcome.DENIED, event.outcome());
        assertEquals(AuditTargetType.REQUEST, event.targetType());
        assertEquals("/rest/users/jdoe", event.target());
        assertEquals(Map.of("method", "DELETE", "reason", "Cannot delete your own account."), event.details());
    }

    @Test
    void securityError_isRecordedInTheAuditLog() {
        var events = new ArrayList<Object>();
        var httpRequest = new MockHttpServletRequest("GET", "/rest/admin/settings");

        var response = advice(events::add).handleSecurityErrors(new AccessDeniedException("Access Denied"),
                new ServletWebRequest(httpRequest));

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        var event = assertInstanceOf(AuditEvent.class, events.getFirst());
        assertEquals(AuditAction.ACCESS_DENIED, event.action());
        assertEquals("/rest/admin/settings", event.target());
        assertEquals("GET", event.details().get("method"));
        assertEquals("Access Denied", event.details().get("reason"));
    }

    @Test
    void failureToRecord_keepsTheAnswer() {
        var advice = advice(event -> {
            throw new IllegalStateException("The database is gone");
        });

        var response = advice.handleSecurityErrors(new AccessDeniedException("Access Denied"),
                new ServletWebRequest(new MockHttpServletRequest("GET", "/rest/admin/settings")));

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    @Test
    void otherErrors_areNotRecordedInTheAuditLog() {
        var events = new ArrayList<Object>();
        var request = new ServletWebRequest(new MockHttpServletRequest("GET", "/rest/users/nobody"));

        var response = advice(events::add).handleAllRestRuntimeExceptions(new NotFoundException("users.message"),
                request);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertTrue(events.isEmpty());
    }

    @Test
    void invalidPath_isMappedToBadRequest() {
        var advice = advice();
        var request = new ServletWebRequest(new MockHttpServletRequest());

        var response = advice.handleInvalidPath(
                new InvalidPathException("/AGENTS.md", "The path cannot be absolute."), request);

        var error = assertInstanceOf(BaseError.class, response.getBody());
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("openl.error.400.default.message", error.code);
    }

    @Test
    void problemDetailUsesItsDetailInsteadOfItsRecordRepresentation() {
        var advice = advice();
        var request = new ServletWebRequest(new MockHttpServletRequest());
        var problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Readable failure");

        var response = advice.handleExceptionInternal(
                new IllegalArgumentException(), problemDetail, new HttpHeaders(), HttpStatus.BAD_REQUEST, request);

        var error = assertInstanceOf(BaseError.class, response.getBody());
        assertEquals("openl.error.400.default.message", error.code);
        assertEquals("Readable failure", error.message);
    }

    @Test
    void committedResponseIsLeftAsItIs() {
        // A body that failed half way through has already been sent in part: there is no error left to write.
        var advice = advice();
        var response = new MockHttpServletResponse();
        response.setCommitted(true);
        var request = new ServletWebRequest(new MockHttpServletRequest(), response);

        var handled = advice.handleExceptionInternal(
                new IllegalStateException("nesting depth"), null, new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR,
                request);

        assertNull(handled);
        assertNull(advice.handleTypeMismatch(new TypeMismatchException("x", Integer.class), new HttpHeaders(),
                HttpStatus.BAD_REQUEST, request), "a type mismatch after the commit is left alone too");
    }

    /** A write refused because another program holds the file open is told as locked, naming the file. */
    @Test
    void workbookLocked_isLockedNamingTheFile() {
        var request = new ServletWebRequest(new MockHttpServletRequest());
        var refused = new FileLockedException("Bank Rating.xlsx",
                new FileSystemException("The process cannot access the file"));

        var response = advice().handleInternalErrors(new IllegalStateException(new UncheckedIOException(refused)),
                request);

        assertEquals(HttpStatus.LOCKED, response.getStatusCode());
        var error = assertInstanceOf(BaseError.class, response.getBody());
        assertEquals("openl.error.423.file.locked.message", error.code);
        assertTrue(error.message.startsWith("Bank Rating.xlsx cannot be changed while another program"),
                error.message);
    }

    /** A file of a repository on the file system that another program holds open is told the same way. */
    @Test
    void fileLocked_isLockedNamingTheFile() {
        var request = new ServletWebRequest(new MockHttpServletRequest());
        var refused = new FileLockedException("rules.xml", new FileSystemException("rules/rules.xml"));

        var response = advice().handleInternalErrors(new IllegalStateException(refused), request);

        assertEquals(HttpStatus.LOCKED, response.getStatusCode());
        var error = assertInstanceOf(BaseError.class, response.getBody());
        assertEquals("openl.error.423.file.locked.message", error.code);
        assertTrue(error.message.startsWith("rules.xml cannot be changed"), error.message);
    }

    /** A write refused because the file changed on disk is a conflict naming the file, however deep it was raised. */
    @Test
    void workbookChangedOutside_isAConflictNamingTheFile() {
        var request = new ServletWebRequest(new MockHttpServletRequest());
        var refused = new WorkbookChangedOutsideException(new File("rules", "Bank Rating.xlsx"));

        var response = advice().handleInternalErrors(new IllegalStateException(new UncheckedIOException(refused)),
                request);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        var error = assertInstanceOf(BaseError.class, response.getBody());
        assertEquals("openl.error.409.workbook.changed.outside.message", error.code);
        assertTrue(error.message.startsWith("Bank Rating.xlsx changed outside OpenL Studio"), error.message);
    }

    @Test
    void problemDetailWithoutDescriptionUsesStatusReason() {
        var advice = advice();
        var request = new ServletWebRequest(new MockHttpServletRequest());
        var problemDetail = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problemDetail.setDetail(" ");
        problemDetail.setTitle(" ");

        var response = advice.handleExceptionInternal(
                new IllegalArgumentException(), problemDetail, new HttpHeaders(), HttpStatus.BAD_REQUEST, request);

        var error = assertInstanceOf(BaseError.class, response.getBody());
        assertEquals("Bad Request", error.message);
    }

    @Test
    void oversizedUploadUsesProblemDetailMessage() throws Exception {
        var advice = advice();
        var request = new ServletWebRequest(new MockHttpServletRequest());

        var response = advice.handleException(new MaxUploadSizeExceededException(1024), request);

        var error = assertInstanceOf(BaseError.class, response.getBody());
        assertEquals(HttpStatus.PAYLOAD_TOO_LARGE, response.getStatusCode());
        assertEquals("openl.error.413.default.message", error.code);
        assertEquals("Maximum upload size exceeded", error.message);
    }

    @Test
    void jettyPartLimitUsesPayloadTooLargeResponse() throws Exception {
        var advice = advice();
        var request = new ServletWebRequest(new MockHttpServletRequest());
        var exception = new MultipartException("Failed to parse multipart servlet request",
                new IllegalStateException("Form with too many keys [4 > 3]"));

        var response = advice.handleMultipartException(exception, request);

        var error = assertInstanceOf(BaseError.class, response.getBody());
        assertEquals(HttpStatus.PAYLOAD_TOO_LARGE, response.getStatusCode());
        assertEquals("openl.error.413.default.message", error.code);
        assertEquals("Maximum upload size exceeded", error.message);
    }

    @Test
    void malformedMultipartUsesBadRequestResponse() throws Exception {
        var advice = advice();
        var request = new ServletWebRequest(new MockHttpServletRequest());

        var response = advice.handleMultipartException(
                new MultipartException("Failed to parse multipart servlet request"), request);

        var error = assertInstanceOf(BaseError.class, response.getBody());
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("openl.error.400.default.message", error.code);
        assertEquals("Failed to parse multipart servlet request", error.message);
    }

    @Test
    void invalidEnumValue_givesFriendlyMessage() {
        assertEquals("Invalid enum format for field 'color'", describeParseFailure("{\"color\":\"PURPLE\"}"));
    }

    @Test
    void wrongType_givesFriendlyMessage() {
        assertEquals("Invalid number format for field 'count'", describeParseFailure("{\"count\":\"abc\"}"));
    }

    @Test
    void unknownField_givesFriendlyMessage() {
        assertEquals("Unknown field 'extra'", describeParseFailure("{\"extra\":1}"));
    }

    @Test
    void malformedJson_givesGenericMessage() {
        assertEquals("Request body is malformed", describeParseFailure("{"));
    }

    private static String describeParseFailure(String json) {
        try {
            new ObjectMapper().readValue(json, Holder.class);
            throw new AssertionError("expected a parse failure for: " + json);
        } catch (JsonProcessingException e) {
            return ApiExceptionControllerAdvice.describeJsonError(e);
        }
    }

    private static ApiExceptionControllerAdvice advice() {
        return advice(event -> {
        });
    }

    private static ApiExceptionControllerAdvice advice(ApplicationEventPublisher eventPublisher) {
        var messageSource = new ReloadableResourceBundleMessageSource();
        messageSource.setBasename("classpath:ValidationMessages");
        messageSource.setDefaultEncoding("UTF-8");
        return new ApiExceptionControllerAdvice(new ExceptionMappingService(messageSource), eventPublisher);
    }

    private enum Color {
        RED, GREEN
    }

    private static final class Holder {
        public Color color;
        public int count;
    }
}
