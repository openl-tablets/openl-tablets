package org.openl.studio.projects.rest;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.function.Supplier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerMapping;

import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.workspace.ProjectIds;
import org.openl.studio.common.exception.ConflictException;
import org.openl.studio.projects.converter.ProjectIdConverter;
import org.openl.studio.projects.rest.annotations.ChangesNothing;
import org.openl.studio.projects.rest.controller.ProjectsController;
import org.openl.studio.projects.rest.controller.ProjectsMergeController;
import org.openl.studio.projects.service.merge.ProjectsMergeLauncher;

/**
 * A project is read-only for a client while the client merges it — see EPBDS-16469.
 */
class MergeInProgressInterceptorTest {

    private static final String PROJECT_ID = ProjectIds.of("design", "RateCalculator").value();

    private ProjectsMergeLauncher mergeLauncher;
    private ProjectIdConverter projectIdConverter;
    private RulesProject project;
    private MergeInProgressInterceptor interceptor;

    @BeforeEach
    void init() {
        mergeLauncher = mock(ProjectsMergeLauncher.class);
        projectIdConverter = mock(ProjectIdConverter.class);
        project = mock(RulesProject.class);
        when(projectIdConverter.convert(PROJECT_ID)).thenReturn(project);
        interceptor = new MergeInProgressInterceptor(mergeLauncher, projectIdConverter);
    }

    private static MockHttpServletRequest request(String method, String projectId) {
        var request = new MockHttpServletRequest(method, "/projects/" + projectId);
        request.setAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE, Map.of("projectId", projectId));
        return request;
    }

    private static HandlerMethod handlerOf(Class<?> controller) {
        var handler = mock(HandlerMethod.class);
        when(handler.getBeanType()).thenAnswer(invocation -> controller);
        return handler;
    }

    private boolean preHandle(String method, Class<?> controller) {
        return interceptor.preHandle(request(method, PROJECT_ID),
                new MockHttpServletResponse(),
                handlerOf(controller));
    }

    /** A merge of the given project by the client runs. */
    private void givenAMergeOf(RulesProject merged) {
        when(mergeLauncher.isRunning(any())).thenAnswer(invocation ->
                invocation.<Supplier<RulesProject>>getArgument(0).get() == merged);
    }

    @Test
    void refusesAChangeOfTheProjectWhileItIsMerged() {
        givenAMergeOf(project);

        for (var method : new String[]{"POST", "PUT", "PATCH", "DELETE"}) {
            assertThrows(ConflictException.class, () -> preHandle(method, ProjectsController.class), method);
        }
    }

    /** The editor stays readable for minutes the merge takes. */
    @Test
    void letsAReadOfTheProjectThroughWhileItIsMerged() {
        givenAMergeOf(project);

        assertTrue(preHandle("GET", ProjectsController.class));
        assertTrue(preHandle("HEAD", ProjectsController.class));
    }

    /**
     * The result, the same merge sent again and the cancel are the business of the merge endpoints themselves. They
     * find an ended merge brought to the editor all the same.
     */
    @Test
    void letsTheEndpointsOfTheMergeThrough() {
        givenAMergeOf(project);

        assertTrue(preHandle("DELETE", ProjectsMergeController.class));
        assertTrue(preHandle("POST", ProjectsMergeController.class));
        verify(mergeLauncher, times(2)).settleFinished();
    }

    /** A compilation, a run, a trace or a comparison changes nothing: the editor keeps showing and running the project. */
    @Test
    void letsARequestThatChangesNothingThrough() throws Exception {
        givenAMergeOf(project);
        var compile = mock(HandlerMethod.class);
        when(compile.hasMethodAnnotation(ChangesNothing.class)).thenReturn(true);

        assertTrue(interceptor.preHandle(request("POST", PROJECT_ID), new MockHttpServletResponse(), compile));
        // The editor compiles a module it opens with a POST, so a page reloaded during a merge still shows the module.
        assertTrue(ProjectsController.class
                .getMethod("compileModule", RulesProject.class, String.class, boolean.class)
                .isAnnotationPresent(ChangesNothing.class));
    }

    @Test
    void letsAChangeOfAnotherProjectThrough() {
        givenAMergeOf(mock(RulesProject.class));

        assertTrue(preHandle("PATCH", ProjectsController.class));
    }

    /** A project is resolved only while a merge of the client runs, which is seldom. */
    @Test
    void resolvesNoProjectWhileNoMergeRuns() {
        assertTrue(preHandle("PATCH", ProjectsController.class));

        verify(projectIdConverter, never()).convert(any());
    }

    /** Any request about a project finds the conflicts of an ended merge kept, before it reads or changes anything. */
    @Test
    void bringsAnEndedMergeToTheEditorFirst() {
        assertTrue(preHandle("GET", ProjectsController.class));

        verify(mergeLauncher).settleFinished();
    }

    @Test
    void leavesARequestAboutNoProjectAlone() {
        var request = new MockHttpServletRequest("POST", "/repos/design/branches");

        assertTrue(interceptor.preHandle(request, new MockHttpServletResponse(), handlerOf(ProjectsController.class)));

        verify(mergeLauncher, never()).settleFinished();
    }
}
