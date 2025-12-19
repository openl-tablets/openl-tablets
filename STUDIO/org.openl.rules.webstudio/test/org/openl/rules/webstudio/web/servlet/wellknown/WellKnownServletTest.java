package org.openl.rules.webstudio.web.servlet.wellknown;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.PrintWriter;
import java.io.StringWriter;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;

import org.openl.rules.spring.openapi.RequestPathUtils;
import org.openl.rules.webstudio.web.servlet.SpringInitializer;

@ExtendWith(MockitoExtension.class)
class WellKnownServletTest {

    private static final String APPLICATION_BASE_URI = "http://localhost:8080";

    private WellKnownServlet servlet;

    @Mock
    private ServletConfig servletConfig;
    @Mock
    private ServletContext servletContext;
    @Mock
    private ApplicationContext applicationContext;
    @Mock
    private Environment environment;

    private MockedStatic<SpringInitializer> springInitializerMock;
    private MockedStatic<RequestPathUtils> requestPathUtilsMock;

    @BeforeEach
    void setUp() throws Exception {
        when(servletConfig.getServletContext()).thenReturn(servletContext);
        when(applicationContext.getEnvironment()).thenReturn(environment);

        springInitializerMock = mockStatic(SpringInitializer.class);
        springInitializerMock
                .when(() -> SpringInitializer.getApplicationContext(servletContext))
                .thenReturn(applicationContext);

        requestPathUtilsMock = mockStatic(RequestPathUtils.class);
        requestPathUtilsMock
                .when(() -> RequestPathUtils.getRequestBasePath(any()))
                .thenReturn(APPLICATION_BASE_URI);

        servlet = new WellKnownServlet();
        servlet.init(servletConfig);
    }

    @AfterEach
    void tearDown() {
        springInitializerMock.close();
        requestPathUtilsMock.close();
    }

    @Test
    void testDoGet_oauth2EnabledAndCorrectPath_returnsProtectedResourceMetadata() throws Exception {
        // given
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);

        StringWriter body = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(body));

        when(environment.getProperty("user.mode")).thenReturn("oauth2");
        when(environment.getProperty("security.oauth2.issuer-uri"))
                .thenReturn("http://issuer.com");
        when(environment.getProperty("security.oauth2.scope"))
                .thenReturn("openid,profile,email");

        when(request.getPathInfo()).thenReturn("/oauth-protected-resource");
        // when
        servlet.doGet(request, response);
        // then
        verify(response).setStatus(HttpServletResponse.SC_OK);
        verify(response).setContentType("application/json");

        String json = body.toString();
        assertTrue(json.contains("\"resource\": \"" + APPLICATION_BASE_URI + "\""));
        assertTrue(json.contains("\"authorization_servers\": [\"http://issuer.com\"]"));
        assertTrue(json.contains("\"scopes_supported\": [\"openid\", \"profile\", \"email\"]"));
    }

    @Test
    void testDoGet_oauth2EnabledWithNullProperties_returnsMetadataWithEmptyArrays() throws Exception {
        // given
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);

        StringWriter body = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(body));

        when(environment.getProperty("user.mode")).thenReturn("oauth2");
        when(environment.getProperty("security.oauth2.issuer-uri"))
                .thenReturn(null);
        when(environment.getProperty("security.oauth2.scope"))
                .thenReturn(null);

        when(request.getPathInfo()).thenReturn("/oauth-protected-resource");
        // when
        servlet.doGet(request, response);
        // then
        verify(response).setStatus(HttpServletResponse.SC_OK);
        verify(response).setContentType("application/json");

        String json = body.toString();
        assertTrue(json.contains("\"resource\": \"http://localhost:8080\""));
        assertTrue(json.contains("\"authorization_servers\": []"));
        assertTrue(json.contains("\"scopes_supported\": []"));
    }

    @Test
    void testDoGet_oauth2Disabled_returns404() throws Exception {
        // given
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);

        when(environment.getProperty("user.mode")).thenReturn("basic");
        // when
        servlet.doGet(request, response);
        // then
        verify(response).sendError(HttpServletResponse.SC_NOT_FOUND);
    }

    @Test
    void testDoGet_unknownPath_returns404() throws Exception {
        // given
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);

        when(environment.getProperty("user.mode")).thenReturn("oauth2");
        when(request.getPathInfo()).thenReturn("/unknown");
        // when
        servlet.doGet(request, response);
        // then
        verify(response).sendError(HttpServletResponse.SC_NOT_FOUND);
    }
}
