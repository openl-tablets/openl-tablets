package org.openl.rules.webstudio.web.servlet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Set;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.core.env.Environment;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletConfig;
import org.springframework.mock.web.MockServletContext;

import org.openl.rules.webstudio.web.Props;

/**
 * @author Yury Molchan
 */
class UserGuidesServletTest {

    private final MockServletContext pages = new MockServletContext(
            "classpath:org/openl/rules/webstudio/web/servlet/pages");
    private final RequestDispatcher container = mock(RequestDispatcher.class);
    private final UserGuidesServlet servlet = new UserGuidesServlet();

    private Environment configured;

    @BeforeEach
    void readTheGuides() throws Exception {
        configured = Props.getEnvironment();
        Props.setEnvironment(new MockEnvironment());
        pages.registerNamedDispatcher("default", container);
        servlet.init(new MockServletConfig(pages));
    }

    @AfterEach
    void leaveTheConfigurationAsItWas() {
        Props.setEnvironment(configured);
    }

    @Test
    void listsThePagesTheWayTheDocumentationSiteDoes() throws Exception {
        var response = get("/toc.json");

        assertEquals("application/json;charset=UTF-8", response.getContentType());
        assertEquals("no-cache", response.getHeader("Cache-Control"));
        var json = new ObjectMapper();
        assertEquals(json.readTree("""
                {"title": "User Guides", "file": "index.md", "children": [
                  {"title": "Getting Started with OpenL Tablets", "file": "getting-started/index.md", "children": [
                    {"title": "Videocasts", "file": "getting-started/videocasts.md"},
                    {"title": "Demo Package Guide", "file": "getting-started/demo-package/index.md"}
                  ]},
                  {"title": "OpenL Studio User Guide", "file": "openl-studio/index.md", "children": [
                    {"title": "Using Rules Editor", "file": "openl-studio/rules-editor.md"},
                    {"title": "Administration", "children": [
                      {"title": "Managing Security Settings",
                       "file": "openl-studio/administration/03-security/index.md", "children": [
                        {"title": "Sso Saml", "file": "openl-studio/administration/03-security/05-sso-saml.md"}
                      ]}
                    ]},
                    {"title": "Appendices", "children": [
                      {"title": "Appendix D: Error Pages", "file": "openl-studio/appendices/error-pages.md"}
                    ]}
                  ]},
                  {"title": "Reference Guide", "children": [
                    {"title": "OpenL Tables Overview", "file": "reference-guide/01-openl-tables-overview.md"}
                  ]}
                ]}
                """), json.readTree(response.getContentAsByteArray()));
    }

    @ParameterizedTest
    @ValueSource(strings = {"/openl-studio/rules-editor.md", "/openl-studio/images/editor.png", "/index.md"})
    void handsAFileOfTheGuidesToTheContainer(String file) throws Exception {
        var response = get(file);

        assertEquals("no-cache", response.getHeader("Cache-Control"));
        var forwarded = forClass(ServletRequest.class);
        verify(container).forward(forwarded.capture(), any());
        var served = (HttpServletRequest) forwarded.getValue();
        assertEquals("/docs" + file, served.getServletPath());
        assertNull(served.getPathInfo());
    }

    @ParameterizedTest
    @ValueSource(strings = {"/openl-studio/rules-editor", "/openl-studio/", "/", ""})
    void drawsAPageOfAGuideOnTheApplicationPage(String page) throws Exception {
        var response = get(page.isEmpty() ? null : page);

        assertEquals(HttpServletResponse.SC_OK, response.getStatus());
        assertTrue(response.getContentAsString().contains("<title>OpenL Studio</title>"));
        verify(container, never()).forward(any(), any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"/missing.md", "/openl-studio/images/missing.png"})
    void findsNoFileTheGuidesDoNotHold(String file) throws Exception {
        var response = get(file);

        assertEquals(HttpServletResponse.SC_NOT_FOUND, response.getStatus());
        verify(container, never()).forward(any(), any());
    }

    @Test
    void answersWithAnErrorWhenTheTableOfContentsCannotBeWritten() throws Exception {
        var response = mock(HttpServletResponse.class);
        when(response.getOutputStream()).thenThrow(new IOException("boom"));

        servlet.service(request("/toc.json"), response);

        verify(response).reset();
        verify(response).setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }

    @Test
    void listsNothingWhereTheWarHoldsNoGuides() throws Exception {
        var empty = new UserGuidesServlet();
        var context = new MockServletContext("classpath:org/openl/rules/webstudio/web/servlet/pages") {
            @Override
            public Set<String> getResourcePaths(String path) {
                return null;
            }
        };
        empty.init(new MockServletConfig(context));
        var response = new MockHttpServletResponse();

        empty.service(request("/toc.json"), response);

        assertEquals("{\"title\":\"Docs\"}", response.getContentAsString());
    }

    private MockHttpServletResponse get(String pathInfo) throws Exception {
        var response = new MockHttpServletResponse();
        servlet.service(request(pathInfo), response);
        return response;
    }

    private MockHttpServletRequest request(String pathInfo) {
        var request = new MockHttpServletRequest(pages, "GET", "/docs" + (pathInfo == null ? "" : pathInfo));
        request.setServletPath("/docs");
        request.setPathInfo(pathInfo);
        return request;
    }
}
