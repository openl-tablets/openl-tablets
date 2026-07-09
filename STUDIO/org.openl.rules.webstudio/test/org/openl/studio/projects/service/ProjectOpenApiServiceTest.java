package org.openl.studio.projects.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;

import org.openl.CompiledOpenClass;
import org.openl.rules.openapi.impl.OpenAPIGeneratedClasses;
import org.openl.rules.openapi.impl.OpenAPIJavaClassGenerator;
import org.openl.rules.openapi.impl.OpenAPIScaffoldingConverter;
import org.openl.rules.project.abstraction.AProjectArtefact;
import org.openl.rules.project.abstraction.AProjectResource;
import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.project.model.Module;
import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.rules.project.model.RulesDeploy;
import org.openl.rules.project.openapi.OpenApiGenerator;
import org.openl.rules.ui.ProjectModel;
import org.openl.rules.webstudio.service.OpenAPIHelper;
import org.openl.security.acl.repository.RepositoryAclService;
import org.openl.studio.common.exception.ConflictException;
import org.openl.studio.common.exception.ForbiddenException;
import org.openl.studio.projects.model.OpenApiTablesRequest;
import org.openl.studio.projects.model.ProjectDescriptorView;
import org.openl.studio.projects.model.ProjectDescriptorView.ModuleView;
import org.openl.studio.projects.model.ProjectDescriptorView.OpenApiView;
import org.openl.studio.projects.service.project.compile.ProjectHandle;

class ProjectOpenApiServiceTest {

    private final RepositoryAclService acl = mock(RepositoryAclService.class);
    private final WorkspaceProjectService workspace = mock(WorkspaceProjectService.class);
    private final ProjectDescriptorService descriptorService = mock(ProjectDescriptorService.class);
    private final ProjectOpenApiService service = new ProjectOpenApiService(acl, workspace, descriptorService);
    private final RulesProject project = mock(RulesProject.class);

    private static ProjectDescriptorView view(List<ModuleView> modules, OpenApiView openapi) {
        return new ProjectDescriptorView("P", null, modules, List.of(), List.of(), openapi, List.of(), null,
                List.of(), true, "hash");
    }

    private static ModuleView module() {
        return new ModuleView("M", "rules/M.xlsx", null, false, false);
    }

    private void compiledModel(boolean hasErrors) {
        var model = mock(ProjectModel.class);
        var handle = mock(ProjectHandle.class);
        when(workspace.openProject(project)).thenReturn(handle);
        when(handle.awaitCompiled()).thenReturn(model);
        var moduleInfo = mock(Module.class);
        when(model.getModuleInfo()).thenReturn(moduleInfo);
        when(moduleInfo.getProject()).thenReturn(mock(ProjectDescriptor.class));
        var compiled = mock(CompiledOpenClass.class);
        when(model.getCompiledOpenClass()).thenReturn(compiled);
        when(compiled.hasErrors()).thenReturn(hasErrors);
    }

    @Test
    void forbidden_without_write_permission() {
        when(acl.isGranted(eq(project), anyList())).thenReturn(false);
        assertThrows(ForbiddenException.class, () -> service.generateSchema(project));
        verifyNoInteractions(workspace);
    }

    @Test
    void rejects_project_without_compiled_modules() throws Exception {
        when(acl.isGranted(eq(project), anyList())).thenReturn(true);
        when(descriptorService.getDescriptor(project)).thenReturn(view(List.of(), null));
        var model = mock(ProjectModel.class);
        var handle = mock(ProjectHandle.class);
        when(workspace.openProject(project)).thenReturn(handle);
        when(handle.awaitCompiled()).thenReturn(model);
        when(model.getModuleInfo()).thenReturn(null);
        assertThrows(ConflictException.class, () -> service.generateSchema(project));
    }

    @Test
    void rejects_compilation_errors() {
        when(acl.isGranted(eq(project), anyList())).thenReturn(true);
        when(descriptorService.getDescriptor(project)).thenReturn(view(List.of(module()), null));
        compiledModel(true);
        assertThrows(ConflictException.class, () -> service.generateSchema(project));
    }

    @Test
    void generates_and_creates_new_schema_file() throws Exception {
        when(acl.isGranted(eq(project), anyList())).thenReturn(true);
        var current = view(List.of(module()), null);
        var finalView = view(List.of(module()), new OpenApiView("openapi.json", "RECONCILIATION", null, null));
        when(descriptorService.getDescriptor(project)).thenReturn(current, finalView);
        compiledModel(false);
        when(project.hasArtefact(anyString())).thenReturn(false);
        var artefact = mock(AProjectArtefact.class);
        when(project.getArtefact("openapi.json")).thenReturn(artefact);
        when(acl.hasAcl(artefact)).thenReturn(false);
        when(acl.createAcl(eq(artefact), anyList(), eq(true))).thenReturn(true);

        try (MockedStatic<OpenApiGenerator> mocked = mockStatic(OpenApiGenerator.class)) {
            var builder = mock(OpenApiGenerator.Builder.class);
            var generator = mock(OpenApiGenerator.class);
            mocked.when(() -> OpenApiGenerator.builder(any(), any())).thenReturn(builder);
            when(builder.generator()).thenReturn(generator);
            when(generator.generate()).thenReturn(new io.swagger.v3.oas.models.OpenAPI());

            var result = service.generateSchema(project);

            assertSame(finalView, result);
            verify(project).addResource(eq("openapi.json"), any(InputStream.class));
            // The default openapi.json is reconciled automatically, so no descriptor rewrite is needed
            // (and none must happen — writing one would drop an auto-discovered project's modules).
            verify(descriptorService, never()).updateDescriptor(any(), any(), anyBoolean());
        }
    }

    @Test
    void overwrites_existing_schema_and_keeps_module_names() throws Exception {
        when(acl.isGranted(eq(project), anyList())).thenReturn(true);
        var current = view(List.of(module()), new OpenApiView("openapi.json", "GENERATION", "Data", "Algo"));
        when(descriptorService.getDescriptor(project)).thenReturn(current, current);
        compiledModel(false);
        when(project.hasArtefact("openapi.json")).thenReturn(true);
        var resource = mock(AProjectResource.class);
        when(project.getArtefact("openapi.json")).thenReturn(resource);

        try (MockedStatic<OpenApiGenerator> mocked = mockStatic(OpenApiGenerator.class)) {
            var builder = mock(OpenApiGenerator.Builder.class);
            var generator = mock(OpenApiGenerator.class);
            mocked.when(() -> OpenApiGenerator.builder(any(), any())).thenReturn(builder);
            when(builder.generator()).thenReturn(generator);
            when(generator.generate()).thenReturn(new io.swagger.v3.oas.models.OpenAPI());

            service.generateSchema(project);

            verify(resource).setContent(any(InputStream.class));
            var captor = ArgumentCaptor.forClass(ProjectDescriptorView.class);
            verify(descriptorService).updateDescriptor(eq(project), captor.capture(), eq(true));
            assertEquals("openapi.json", captor.getValue().openapi().path());
            assertEquals("RECONCILIATION", captor.getValue().openapi().mode());
            assertEquals("Data", captor.getValue().openapi().modelModuleName());
            assertEquals("Algo", captor.getValue().openapi().algorithmModuleName());
        }
    }

    private static OpenApiTablesRequest tablesRequest(String rulesPath, String dataPath) {
        return new OpenApiTablesRequest("openapi.json", "Rules", "Data", rulesPath, dataPath);
    }

    @Test
    void tables_forbidden_without_write_permission() {
        when(acl.isGranted(eq(project), anyList())).thenReturn(false);
        assertThrows(ForbiddenException.class,
                () -> service.generateTables(project, tablesRequest("rules/Rules.xlsx", "rules/Data.xlsx")));
    }

    @Test
    void tables_rejects_equal_module_paths() {
        when(acl.isGranted(eq(project), anyList())).thenReturn(true);
        assertThrows(ConflictException.class,
                () -> service.generateTables(project, tablesRequest("rules/Same.xlsx", "rules/Same.xlsx")));
    }

    @Test
    void tables_rejects_missing_spec() {
        when(acl.isGranted(eq(project), anyList())).thenReturn(true);
        when(project.hasArtefact("openapi.json")).thenReturn(false);
        assertThrows(ConflictException.class,
                () -> service.generateTables(project, tablesRequest("rules/Rules.xlsx", "rules/Data.xlsx")));
    }

    @Test
    void tables_generates_modules_and_writes_generation_descriptor() throws Exception {
        when(acl.isGranted(eq(project), anyList())).thenReturn(true);
        var current = view(List.of(), null);
        var finalView = view(List.of(module()), new OpenApiView("openapi.json", "GENERATION", "Data", "Rules"));
        when(descriptorService.getDescriptor(project)).thenReturn(current, finalView);
        when(project.hasArtefact(anyString())).thenReturn(false);
        when(project.hasArtefact("openapi.json")).thenReturn(true);
        var spec = mock(AProjectResource.class);
        when(project.getArtefact("openapi.json")).thenReturn(spec);
        when(spec.getContent()).thenReturn(new ByteArrayInputStream("{}".getBytes()));
        var moduleArtefact = mock(AProjectArtefact.class);
        when(project.getArtefact("rules/Rules.xlsx")).thenReturn(moduleArtefact);
        when(project.getArtefact("rules/Data.xlsx")).thenReturn(moduleArtefact);
        when(acl.hasAcl(moduleArtefact)).thenReturn(true);

        var scaffolding = mock(org.openl.rules.model.scaffolding.ProjectModel.class);
        when(scaffolding.getDatatypeModels()).thenReturn(Collections.emptySet());
        when(scaffolding.getSpreadsheetResultModels()).thenReturn(Collections.emptyList());
        when(scaffolding.getDataModels()).thenReturn(Collections.emptyList());
        when(scaffolding.getIncludeMethodFilter()).thenReturn(Collections.emptySet());
        var generated = mock(OpenAPIGeneratedClasses.class);
        when(generated.hasAnnotationTemplateClass()).thenReturn(false);
        when(generated.getGroovyCommonClasses()).thenReturn(Collections.emptySet());

        try (MockedConstruction<OpenAPIScaffoldingConverter> converter = mockConstruction(OpenAPIScaffoldingConverter.class,
                (m, c) -> when(m.extractProjectModel(anyString())).thenReturn(scaffolding));
                MockedConstruction<OpenAPIJavaClassGenerator> gen = mockConstruction(OpenAPIJavaClassGenerator.class,
                        (m, c) -> when(m.generate()).thenReturn(generated));
                MockedConstruction<OpenAPIHelper> helper = mockConstruction(OpenAPIHelper.class, (m, c) -> {
                    when(m.generateDataTypesFile(any())).thenReturn(new ByteArrayInputStream(new byte[0]));
                    when(m.generateAlgorithmsModule(any(), any(), any())).thenReturn(new ByteArrayInputStream(new byte[0]));
                    when(m.editOrCreateRulesDeploy(any(), any(), any())).thenReturn(new ByteArrayInputStream(new byte[0]));
                })) {

            var result = service.generateTables(project, tablesRequest("rules/Rules.xlsx", "rules/Data.xlsx"));

            assertSame(finalView, result);
            verify(project).addResource(eq("rules/Rules.xlsx"), any(InputStream.class));
            verify(project).addResource(eq("rules/Data.xlsx"), any(InputStream.class));
            verify(project).addResource(eq(RulesDeploy.FILE_NAME), any(InputStream.class));
            var captor = ArgumentCaptor.forClass(ProjectDescriptorView.class);
            verify(descriptorService).updateDescriptor(eq(project), captor.capture(), eq(true));
            var written = captor.getValue();
            assertEquals("GENERATION", written.openapi().mode());
            assertEquals("openapi.json", written.openapi().path());
            assertEquals(2, written.modules().size());
        }
    }
}
