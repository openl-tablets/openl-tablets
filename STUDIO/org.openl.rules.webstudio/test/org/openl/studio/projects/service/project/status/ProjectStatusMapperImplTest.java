package org.openl.studio.projects.service.project.status;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.openl.dependency.CompiledDependency;
import org.openl.message.OpenLMessage;
import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.project.instantiation.IDependencyLoader;
import org.openl.rules.project.model.Module;
import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.rules.project.model.WebstudioConfiguration;
import org.openl.rules.ui.ProjectCompilationStatus;
import org.openl.rules.ui.ProjectModel;
import org.openl.rules.webstudio.dependencies.WebStudioWorkspaceRelatedDependencyManager;
import org.openl.studio.projects.model.ProjectIdModel;
import org.openl.studio.projects.model.project.status.CompileState;
import org.openl.studio.projects.service.DetailedMessageDescriptionMapper;
import org.openl.studio.projects.service.ProjectIdentifierMapper;
import org.openl.studio.projects.service.project.changes.PendingChangesResolver;
import org.openl.studio.projects.service.project.compile.CompilationJob;
import org.openl.studio.projects.service.project.compile.CompilationJobRegistry;

class ProjectStatusMapperImplTest {

    private CompilationJobRegistry compilationJobRegistry;
    private PendingChangesResolver pendingChangesResolver;
    private DetailedMessageDescriptionMapper detailedMessageDescriptionMapper;
    private ProjectStatusMapperImpl mapper;

    private RulesProject project;
    private ProjectModel model;

    @BeforeEach
    void setUp() {
        var projectIdentifierMapper = mock(ProjectIdentifierMapper.class);
        compilationJobRegistry = mock(CompilationJobRegistry.class);
        pendingChangesResolver = mock(PendingChangesResolver.class);
        detailedMessageDescriptionMapper = mock(DetailedMessageDescriptionMapper.class);
        mapper = new ProjectStatusMapperImpl(projectIdentifierMapper, compilationJobRegistry, pendingChangesResolver,
                detailedMessageDescriptionMapper);

        project = mock(RulesProject.class);
        when(project.isSupportsBranches()).thenReturn(false);

        // A completed compile with 3 messages: 2 errors and 1 warning.
        var compilationStatus = mock(ProjectCompilationStatus.class);
        when(compilationStatus.getAllMessage())
                .thenReturn(List.of(mock(OpenLMessage.class), mock(OpenLMessage.class), mock(OpenLMessage.class)));
        when(compilationStatus.getErrorsCount()).thenReturn(2);
        when(compilationStatus.getWarningsCount()).thenReturn(1);
        when(compilationStatus.getModulesCount()).thenReturn(1);
        when(compilationStatus.getModulesCompiled()).thenReturn(1);

        // A single-module compile so the compiled-module names resolve without walking the loader graph.
        var config = mock(WebstudioConfiguration.class);
        when(config.isCompileThisModuleOnly()).thenReturn(true);
        var module = mock(Module.class);
        when(module.getName()).thenReturn("Main");
        when(module.getWebstudioConfiguration()).thenReturn(config);

        model = mock(ProjectModel.class);
        when(model.getCompilationStatus()).thenReturn(compilationStatus);
        when(model.isCompilationInProgress()).thenReturn(false);
        when(model.isProjectCompilationCompleted()).thenReturn(true);
        when(model.isOpenedModuleCompiled()).thenReturn(true);
        when(model.getModuleInfo()).thenReturn(module);

        when(projectIdentifierMapper.map(project)).thenReturn(mock(ProjectIdModel.class));
        when(detailedMessageDescriptionMapper.mapSorted(any(), any())).thenReturn(List.of());
    }

    @Test
    void detailedMapResolvesTheMessageList() {
        var status = mapper.map(project, model);

        var messages = status.compilation().messages();
        assertNotNull(messages.items());
        assertEquals(3, messages.total());
        assertEquals(2, messages.errors());
        assertEquals(1, messages.warnings());
        assertEquals(List.of("Main"), status.compilation().modules().compiledModules());
        assertEquals(CompileState.ERRORS, status.compileState());
        verify(detailedMessageDescriptionMapper).mapSorted(any(), any());
    }

    @Test
    void summaryReportsCountsWithoutResolvingTheMessageList() {
        var job = mock(CompilationJob.class);
        when(job.project()).thenReturn(model);
        when(compilationJobRegistry.find(any(), any())).thenReturn(Optional.of(job));

        var status = mapper.mapSummary(project);

        var messages = status.compilation().messages();
        assertNull(messages.items());
        assertEquals(3, messages.total());
        assertEquals(2, messages.errors());
        assertEquals(1, messages.warnings());
        // Module counts stay, but the compiled-module names (a loader-graph walk) are skipped too.
        var modules = status.compilation().modules();
        assertNull(modules.compiledModules());
        assertEquals(1, modules.total());
        assertEquals(1, modules.compiled());
        assertEquals(CompileState.ERRORS, status.compileState());
        // The point of the summary: never resolve each message to its table and module.
        verify(detailedMessageDescriptionMapper, never()).mapSorted(any(), any());
    }

    @Test
    void progressNamesTheModulesBuiltSoFarWithoutTheWorkACompilationCannotAnswerFor() {
        var status = mapper.mapProgress(project, model);

        // What the reader is waiting to see: how far the compilation has come.
        assertEquals(List.of("Main"), status.compilation().modules().compiledModules());
        assertEquals(1, status.compilation().modules().total());
        assertEquals(1, status.compilation().modules().compiled());
        assertEquals(3, status.compilation().messages().total());
        // Resolving each message to its table reads the model under the lock the compilation holds, and
        // what is not committed yet is read from disk — neither is worth the wait for a progress report.
        assertNull(status.compilation().messages().items());
        assertNull(status.pendingChanges());
        // Counting the tests walks every method of what is compiled so far, which is the same kind of work.
        assertNull(status.compilation().tests());
        verify(detailedMessageDescriptionMapper, never()).mapSorted(any(), any());
        verify(pendingChangesResolver, never()).resolve(any());
        verify(model, never()).getAllTestMethods();
    }

    @Test
    void theModuleBeingOpenedIsNamedOnlyOnceItIsCompiled() {
        var project = new ProjectDescriptor();
        var opened = module("Claims", project);
        var other = module("Pricing", project);
        var openedLoader = loader(opened, null);
        var otherLoader = loader(other, mock(CompiledDependency.class));
        var dependencyManager = mock(WebStudioWorkspaceRelatedDependencyManager.class);
        when(dependencyManager.findAllProjectDependencyLoaders(project)).thenReturn(List.of(openedLoader, otherLoader));
        when(model.getWebStudioWorkspaceDependencyManager()).thenReturn(dependencyManager);
        when(model.getModuleInfo()).thenReturn(opened);
        when(model.isProjectCompilationCompleted()).thenReturn(false);

        // Its own compilation is what the reader is waiting for, so while it runs the module is not named.
        when(model.isOpenedModuleCompiled()).thenReturn(false);
        assertEquals(List.of("Pricing"), mapper.map(this.project, model).compilation().modules().compiledModules());

        when(model.isOpenedModuleCompiled()).thenReturn(true);
        assertEquals(List.of("Claims", "Pricing"),
                mapper.map(this.project, model).compilation().modules().compiledModules());
    }

    /** A module of the given project, declared the way a project descriptor declares one. */
    private static Module module(String name, ProjectDescriptor project) {
        var module = new Module();
        module.setName(name);
        module.setProject(project);
        return module;
    }

    /** The loader of one module, carrying its compiled result when it has one. */
    private static IDependencyLoader loader(Module module, CompiledDependency compiled) {
        var loader = mock(IDependencyLoader.class);
        when(loader.isProjectLoader()).thenReturn(false);
        when(loader.getModule()).thenReturn(module);
        when(loader.getProject()).thenReturn(module.getProject());
        when(loader.getRefToCompiledDependency()).thenReturn(compiled);
        return loader;
    }
}
