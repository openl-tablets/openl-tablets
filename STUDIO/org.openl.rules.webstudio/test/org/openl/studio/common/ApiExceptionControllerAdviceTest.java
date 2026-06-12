package org.openl.studio.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

import java.nio.file.InvalidPathException;

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
}
