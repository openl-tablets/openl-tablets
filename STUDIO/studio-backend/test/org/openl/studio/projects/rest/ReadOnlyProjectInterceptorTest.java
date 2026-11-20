package org.openl.studio.projects.rest;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerMapping;

import org.openl.rules.workspace.ProjectIds;
import org.openl.studio.common.exception.ConflictException;
import org.openl.studio.projects.rest.annotations.ChangesNothing;

/**
 * A project downloaded from the Maven repository is read through the REST API, and never changed through it.
 *
 * @author Yury Molchan
 */
class ReadOnlyProjectInterceptorTest {

    private static final String MAVEN_ID = "maven:com.example:domain:1.0.0";

    private final ReadOnlyProjectInterceptor interceptor = new ReadOnlyProjectInterceptor();

    private static MockHttpServletRequest request(String method, String projectId) {
        var request = new MockHttpServletRequest(method, "/projects/" + projectId);
        request.setAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE, Map.of("projectId", projectId));
        return request;
    }

    private boolean preHandle(String method, String projectId, Object handler) {
        return interceptor.preHandle(request(method, projectId), new MockHttpServletResponse(), handler);
    }

    @ParameterizedTest
    @ValueSource(strings = {"POST", "PUT", "PATCH", "DELETE"})
    void refusesAWriteToADownloadedProject(String method) {
        var handler = mock(HandlerMethod.class);

        var refusal = assertThrows(ConflictException.class, () -> preHandle(method, MAVEN_ID, handler));

        assertEquals("openl.error.409.project.read-only.message", refusal.getErrorCode());
        assertArrayEquals(new Object[]{"com.example:domain:1.0.0"}, refusal.getArgs());
    }

    @ParameterizedTest
    @ValueSource(strings = {"GET", "HEAD", "OPTIONS"})
    void letsAReadOfADownloadedProjectThrough(String method) {
        assertTrue(preHandle(method, MAVEN_ID, mock(HandlerMethod.class)));
    }

    /** A compilation, a run, a test or a trace is sent as a POST, yet changes nothing. */
    @Test
    void letsARequestThatChangesNothingThrough() {
        var compile = mock(HandlerMethod.class);
        when(compile.hasMethodAnnotation(ChangesNothing.class)).thenReturn(true);

        assertTrue(preHandle("POST", MAVEN_ID, compile));
    }

    @Test
    void refusesAWriteWhateverServesIt() {
        assertThrows(ConflictException.class, () -> preHandle("PUT", MAVEN_ID, new Object()));
    }

    @Test
    void letsAWriteToAProjectOfADesignRepositoryThrough() {
        var projectId = ProjectIds.of("design", "RateCalculator").value();

        assertTrue(preHandle("PUT", projectId, mock(HandlerMethod.class)));
    }

    @Test
    void letsAnIncompleteMavenIdThroughToBeAnsweredAsUnknown() {
        assertTrue(preHandle("DELETE", "maven:com.example:domain", mock(HandlerMethod.class)));
    }

    @Test
    void leavesARequestAboutNoProjectAlone() {
        var request = new MockHttpServletRequest("PUT", "/repos/design/branches/main");
        request.setAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE, Map.of("repo-name", "design"));

        assertTrue(interceptor.preHandle(request, new MockHttpServletResponse(), mock(HandlerMethod.class)));
    }

    @Test
    void leavesARequestOfNoAddressVariablesAlone() {
        var request = new MockHttpServletRequest("POST", "/repos/design/branches");

        assertTrue(interceptor.preHandle(request, new MockHttpServletResponse(), mock(HandlerMethod.class)));
    }
}
