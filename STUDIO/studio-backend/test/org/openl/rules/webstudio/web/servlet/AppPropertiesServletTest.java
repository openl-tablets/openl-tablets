package org.openl.rules.webstudio.web.servlet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import jakarta.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;

class AppPropertiesServletTest {

    private final AppPropertiesServlet servlet = new AppPropertiesServlet();
    private final MockServletContext servletContext = new MockServletContext();

    @Test
    void writesTheDefaultPropertiesAsPlainText() throws Exception {
        var response = new MockHttpServletResponse();

        servlet.service(request(), response);

        assertEquals(200, response.getStatus());
        assertTrue(response.getContentType().startsWith("text/plain"), response.getContentType());
        assertTrue(response.getContentAsString().contains("This file was generated"),
                response.getContentAsString());
    }

    @Test
    void answersWithAnErrorWhenTheResponseStreamIsGone() throws Exception {
        var response = mock(HttpServletResponse.class);
        when(response.getOutputStream()).thenThrow(new IOException("Connection closed"));

        servlet.service(request(), response);

        verify(response).reset();
        verify(response).setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }

    private MockHttpServletRequest request() {
        return new MockHttpServletRequest(servletContext, "GET", "/application.properties");
    }
}
