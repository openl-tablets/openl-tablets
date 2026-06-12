package org.openl.studio.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

import java.nio.file.InvalidPathException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.ServletWebRequest;

class ApiExceptionControllerAdviceTest {

    @Test
    void invalidPath_isMappedToBadRequest() {
        var advice = new ApiExceptionControllerAdvice(mock(ExceptionMappingService.class));
        var request = new ServletWebRequest(new MockHttpServletRequest());

        var response = advice.handleInvalidPath(
                new InvalidPathException("/AGENTS.md", "The path cannot be absolute."), request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
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

    private enum Color {
        RED, GREEN
    }

    private static final class Holder {
        public Color color;
        public int count;
    }
}
