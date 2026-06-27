package org.openl.rules.webstudio.web.tab;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.project.model.Module;
import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.rules.ui.ProjectModel;
import org.openl.rules.ui.WebStudio;
import org.openl.rules.webstudio.web.util.WebStudioUtils;

/**
 * Unit tests for resolving a tab's project/module/model from the identity it sends, using the session's
 * opened-project registry without mutating the session-global selection.
 */
class TabContextResolverTest {

    private static WebStudio studioWith(ProjectDescriptor descriptor, RulesProject project, Module module) {
        WebStudio studio = mock(WebStudio.class);
        when(studio.getProjectByName("design", "A")).thenReturn(descriptor);
        when(studio.getProject("design", "ProjA")).thenReturn(project);
        when(studio.getModule(descriptor, "M")).thenReturn(module);
        return studio;
    }

    private static ProjectDescriptor descriptorAt(String folder) {
        ProjectDescriptor descriptor = mock(ProjectDescriptor.class);
        when(descriptor.getProjectFolder()).thenReturn(Path.of("/ws", folder));
        return descriptor;
    }

    @Test
    void resolvesCachedModelWithoutMutatingSelection() {
        var descriptor = descriptorAt("ProjA");
        var project = mock(RulesProject.class);
        var module = mock(Module.class);
        var model = mock(ProjectModel.class);
        var studio = studioWith(descriptor, project, module);
        when(studio.getModelIfPresent(project)).thenReturn(model);

        try (var utils = mockStatic(WebStudioUtils.class)) {
            utils.when(WebStudioUtils::getWebStudio).thenReturn(studio);
            TabContext ctx = TabContextResolver.resolve("design", "A", "M");

            assertSame(model, ctx.getModel());
            assertSame(project, ctx.getProject());
            assertSame(descriptor, ctx.getProjectDescriptor());
            assertSame(module, ctx.getModule());
        }
        verify(studio, never()).init(any(), any(), any(), any());
        verify(studio, never()).openProjectModule(any(), any(), any());
    }

    @Test
    void opensModuleWhenModelNotCached() {
        var descriptor = descriptorAt("ProjA");
        var project = mock(RulesProject.class);
        var module = mock(Module.class);
        var model = mock(ProjectModel.class);
        var studio = studioWith(descriptor, project, module);
        when(studio.getModelIfPresent(project)).thenReturn(null);
        when(studio.openProjectModule(project, descriptor, module)).thenReturn(model);

        try (var utils = mockStatic(WebStudioUtils.class)) {
            utils.when(WebStudioUtils::getWebStudio).thenReturn(studio);
            TabContext ctx = TabContextResolver.resolve("design", "A", "M");
            assertSame(model, ctx.getModel());
        }
        verify(studio).openProjectModule(project, descriptor, module);
    }

    @Test
    void returnsNullWhenNoSessionStudio() {
        try (var utils = mockStatic(WebStudioUtils.class)) {
            utils.when(WebStudioUtils::getWebStudio).thenReturn(null);
            assertNull(TabContextResolver.resolve("design", "A", "M"));
        }
    }

    @Test
    void returnsNullWhenProjectNotFound() {
        WebStudio studio = mock(WebStudio.class);
        when(studio.getProjectByName("design", "A")).thenReturn(null);
        try (var utils = mockStatic(WebStudioUtils.class)) {
            utils.when(WebStudioUtils::getWebStudio).thenReturn(studio);
            assertNull(TabContextResolver.resolve("design", "A", "M"));
        }
    }

    @Test
    void returnsNullForMissingIdentity() {
        assertNull(TabContextResolver.resolve(null, "A", "M"));
        assertNull(TabContextResolver.resolve("design", null, "M"));
    }

    @Test
    void returnsNullWhenRulesProjectUnavailable() {
        var descriptor = descriptorAt("ProjA");
        WebStudio studio = mock(WebStudio.class);
        when(studio.getProjectByName("design", "A")).thenReturn(descriptor);
        when(studio.getProject("design", "ProjA")).thenReturn(null);
        try (var utils = mockStatic(WebStudioUtils.class)) {
            utils.when(WebStudioUtils::getWebStudio).thenReturn(studio);
            assertNull(TabContextResolver.resolve("design", "A", "M"));
        }
    }

    @Test
    void returnsNullWhenResolutionThrows() {
        WebStudio studio = mock(WebStudio.class);
        when(studio.getProjectByName("design", "A")).thenThrow(new RuntimeException("boom"));
        try (var utils = mockStatic(WebStudioUtils.class)) {
            utils.when(WebStudioUtils::getWebStudio).thenReturn(studio);
            assertNull(TabContextResolver.resolve("design", "A", "M"));
        }
    }
}
