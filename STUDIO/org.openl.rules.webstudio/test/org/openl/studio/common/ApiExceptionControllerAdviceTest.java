package org.openl.studio.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import java.nio.file.InvalidPathException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;

import org.openl.studio.common.model.BaseError;

class ApiExceptionControllerAdviceTest {

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
        var messageSource = new ReloadableResourceBundleMessageSource();
        messageSource.setBasename("classpath:ValidationMessages");
        messageSource.setDefaultEncoding("UTF-8");
        return new ApiExceptionControllerAdvice(new ExceptionMappingService(messageSource));
    }

    private enum Color {
        RED, GREEN
    }

    private static final class Holder {
        public Color color;
        public int count;
    }
}
