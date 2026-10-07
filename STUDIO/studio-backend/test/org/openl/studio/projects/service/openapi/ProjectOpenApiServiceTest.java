package org.openl.studio.projects.service.openapi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;

import org.openl.CompiledOpenClass;
import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.project.model.Module;
import org.openl.rules.project.model.OpenAPI;
import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.rules.ui.ProjectModel;
import org.openl.studio.common.exception.ConflictException;
import org.openl.studio.projects.service.WorkspaceProjectService;
import org.openl.studio.projects.service.files.ProjectFileRootFactory;
import org.openl.studio.projects.service.files.ProjectFilesService;
import org.openl.studio.projects.service.project.compile.ProjectHandle;

/**
 * What a project is refused a specification for, and which of its files the specification is written to.
 *
 * <p>The document itself is the engine's to generate; that it is written where the project keeps one, and
 * that a project with nothing to describe is told so rather than given an empty specification, is this
 * service's own.
 */
class ProjectOpenApiServiceTest {

    private final WorkspaceProjectService projects = mock(WorkspaceProjectService.class);
    private final ProjectFilesService files = mock(ProjectFilesService.class);
    private final ProjectOpenApiService service = new ProjectOpenApiService(projects, files,
            mock(ProjectFileRootFactory.class));

    @Test
    void refusesAProjectThatDeclaresNoModules() {
        var project = resolvedTo(described("Rates"));

        var refused = assertThrows(ConflictException.class, () -> service.writeSchema(project));

        assertEquals("openl.error.409.projects.openapi.no-modules.message", refused.getErrorCode());
        // Nothing of the project is written: a specification of no rules describes nothing.
        verify(files, never()).createResource(any(), any(), any(), anyBoolean());
    }

    @Test
    void refusesAProjectThatDidNotCompile() {
        var compiled = mock(CompiledOpenClass.class);
        when(compiled.hasErrors()).thenReturn(true);
        var project = openedWith(described("Rates", new Module()), compiled);

        var refused = assertThrows(ConflictException.class, () -> service.writeSchema(project));

        assertEquals("openl.error.409.projects.openapi.not-compiled.message", refused.getErrorCode());
    }

    @Test
    void writesOverTheSpecificationTheDescriptorNames() {
        var declared = described("Rates", new Module());
        var openapi = new OpenAPI();
        openapi.setPath("api/rates.yaml");
        declared.setOpenapi(openapi);
        var project = mock(RulesProject.class);
        when(project.hasArtefact("api/rates.yaml")).thenReturn(true);

        assertEquals("api/rates.yaml", ProjectOpenApiService.writtenSchemaPath(project, declared));
    }

    @Test
    void writesOverTheSpecificationTheProjectHoldsUnderAKnownName() {
        var project = mock(RulesProject.class);
        when(project.hasArtefact("openapi.yaml")).thenReturn(true);

        // The descriptor names none, so the file the engine would read is the one written over.
        assertEquals("openapi.yaml", ProjectOpenApiService.writtenSchemaPath(project, described("Rates", new Module())));
    }

    @Test
    void namesNothingForAProjectThatKeepsNoSpecification() {
        assertNull(ProjectOpenApiService.writtenSchemaPath(mock(RulesProject.class), null));
    }

    @Test
    void ignoresAPathTheProjectDoesNotHold() {
        var declared = described("Rates", new Module());
        var openapi = new OpenAPI();
        openapi.setPath("api/gone.yaml");
        declared.setOpenapi(openapi);

        // A descriptor naming a file nobody wrote is not a file to write over; the project is given one afresh.
        assertNull(ProjectOpenApiService.writtenSchemaPath(mock(RulesProject.class), declared));
    }

    /** A descriptor declaring the given modules under a name. */
    static ProjectDescriptor described(String name, Module... modules) {
        var descriptor = new ProjectDescriptor();
        descriptor.setName(name);
        descriptor.setModules(List.of(modules));
        return descriptor;
    }

    /** A project the engine resolves to the given descriptor, without opening it. */
    private RulesProject resolvedTo(ProjectDescriptor resolved) {
        var project = mock(RulesProject.class);
        when(projects.getProjectDescriptor(project)).thenReturn(resolved);
        return project;
    }

    /** A project resolved to that descriptor, whose compilation has finished with those compiled classes. */
    private RulesProject openedWith(ProjectDescriptor resolved, CompiledOpenClass compiled) {
        var project = resolvedTo(resolved);
        var model = mock(ProjectModel.class);
        when(model.getCompiledOpenClass()).thenReturn(compiled);
        var handle = mock(ProjectHandle.class);
        when(handle.awaitCompiled()).thenReturn(model);
        when(projects.openProject(project)).thenReturn(handle);
        return project;
    }
}
