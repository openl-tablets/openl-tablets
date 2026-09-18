package org.openl.rules.webstudio.web.servlet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.MappingMatch;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletConfig;
import org.springframework.mock.web.MockServletContext;

/**
 * @author Yury Molchan
 */
class StaticResourcesServletTest {

    private final MockServletContext servletContext = new MockServletContext();
    private final StaticResourcesServlet servlet = new StaticResourcesServlet();

    /**
     * Whichever mapping matched, the container is handed the whole address the way it reports one of its own -
     * it reads the servlet path of such a request, and a half address finds no file.
     */
    @ParameterizedTest
    @CsvSource({
            "/favicon.svg, /favicon.svg, ",
            "/favicon.ico, /favicon.ico, ",
            "/assets/index-abc.js, /assets, /index-abc.js",
            "/icons/site.webmanifest, /icons, /site.webmanifest"
    })
    void handsTheWholeAddressToTheContainer(String address, String servletPath, String pathInfo) throws Exception {
        var dispatcher = mock(RequestDispatcher.class);
        serve(dispatcher, address, servletPath, pathInfo, new MockHttpServletResponse());

        var forwarded = forClass(ServletRequest.class);
        verify(dispatcher).forward(forwarded.capture(), any());
        var served = (HttpServletRequest) forwarded.getValue();
        assertEquals(address, served.getServletPath());
        assertNull(served.getPathInfo());
        assertEquals(MappingMatch.DEFAULT, served.getHttpServletMapping().getMappingMatch());
    }

    @Test
    void answersWithAnErrorWhenTheContainerCannotServeTheFile() throws Exception {
        var dispatcher = mock(RequestDispatcher.class);
        doThrow(new ServletException("boom")).when(dispatcher).forward(any(), any());
        var response = mock(HttpServletResponse.class);

        serve(dispatcher, "/assets/index-abc.js", "/assets", "/index-abc.js", response);

        verify(response).reset();
        verify(response).setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }

    private void serve(RequestDispatcher dispatcher,
                       String address,
                       String servletPath,
                       String pathInfo,
                       HttpServletResponse response) throws Exception {
        servletContext.registerNamedDispatcher("default", dispatcher);
        servlet.init(new MockServletConfig(servletContext));
        var request = new MockHttpServletRequest(servletContext, "GET", address);
        request.setServletPath(servletPath);
        request.setPathInfo(pathInfo);
        servlet.service(request, response);
    }
}
