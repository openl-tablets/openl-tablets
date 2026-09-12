package org.openl.studio.projects.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.openl.message.OpenLMessage;
import org.openl.message.Severity;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.project.instantiation.IDependencyLoader;
import org.openl.rules.project.model.Module;
import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.rules.table.xls.XlsUrlParser;
import org.openl.rules.ui.ProjectModel;
import org.openl.rules.ui.WebStudio;
import org.openl.rules.webstudio.dependencies.WebStudioWorkspaceRelatedDependencyManager;
import org.openl.studio.projects.model.ProjectIdModel;
import org.openl.studio.projects.model.project.status.ModuleMessageSource;
import org.openl.studio.projects.model.project.status.TableMessageSource;

class DetailedMessageDescriptionMapperImplTest {

    private final ProjectIdentifierMapper projectIdentifierMapper = mock(ProjectIdentifierMapper.class);
    private final DetailedMessageDescriptionMapperImpl mapper = new DetailedMessageDescriptionMapperImpl(
            projectIdentifierMapper);

    private ProjectModel model;

    @BeforeEach
    void setUp() {
        model = mock(ProjectModel.class);
        when(model.getAllTableSyntaxNodes()).thenReturn(Set.of());
        when(model.getWebStudioWorkspaceDependencyManager()).thenReturn(null);
    }

    /** A module of the workspace, named and sitting at the location its tables are addressed under. */
    private static Module module(String name, String uri) {
        var module = mock(Module.class);
        when(module.getName()).thenReturn(name);
        when(module.getRulesRootPath()).thenReturn(uri);
        when(module.getRelativeUri()).thenReturn(uri);
        return module;
    }

    /** A workspace that has compiled the given modules, as the dependency loaders report them. */
    private static WebStudioWorkspaceRelatedDependencyManager workspaceOf(Module... modules) {
        var loaders = Arrays.stream(modules).map(module -> {
            var loader = mock(IDependencyLoader.class);
            when(loader.isProjectLoader()).thenReturn(false);
            when(loader.getModule()).thenReturn(module);
            return loader;
        }).toList();
        var dependencyManager = mock(WebStudioWorkspaceRelatedDependencyManager.class);
        when(dependencyManager.getDependencyLoaders()).thenReturn(List.copyOf(loaders));
        return dependencyManager;
    }

    private static OpenLMessage message(String sourceLocation) {
        var message = mock(OpenLMessage.class);
        when(message.getSourceLocation()).thenReturn(sourceLocation);
        when(message.getSeverity()).thenReturn(Severity.ERROR);
        return message;
    }

    @Test
    void indexesTablesAndModulesOncePerCallNotPerMessage() {
        mapper.mapSorted(List.of(message("u1"), message("u2"), message("u3")), model);

        // The whole point of the fix: index the model once, not rescan it for every message.
        verify(model, times(1)).getAllTableSyntaxNodes();
        verify(model, times(1)).getWebStudioWorkspaceDependencyManager();
    }

    @Test
    void resolvesAMessageToItsOwningModule() {
        var dependencyManager = workspaceOf(module("Rating", "uri"));
        when(model.getWebStudioWorkspaceDependencyManager()).thenReturn(dependencyManager);

        var result = mapper.mapSorted(List.of(message("uri")), model);

        assertEquals(1, result.size());
        var location = assertInstanceOf(ModuleMessageSource.class, result.getFirst().location());
        assertEquals("Rating", location.name());
    }

    @Test
    void skipsProjectLoadersWhenResolvingTheModule() {
        var projectLoader = mock(IDependencyLoader.class);
        when(projectLoader.isProjectLoader()).thenReturn(true);
        var dependencyManager = mock(WebStudioWorkspaceRelatedDependencyManager.class);
        when(dependencyManager.getDependencyLoaders()).thenReturn(List.of(projectLoader));
        when(model.getWebStudioWorkspaceDependencyManager()).thenReturn(dependencyManager);

        var result = mapper.mapSorted(List.of(message("uri")), model);

        assertNull(result.getFirst().location());
        verify(projectLoader, times(0)).getModule();
    }

    @Test
    void resolvesAMessageToTheTableItIntersects() {
        var node = mock(TableSyntaxNode.class);
        when(node.getId()).thenReturn("n1");
        when(node.getUriParser()).thenReturn(new XlsUrlParser("file:/wb.xlsx?sheet=Sheet1&range=A1:C3"));
        when(model.getAllTableSyntaxNodes()).thenReturn(Set.of(node));

        var result = mapper.mapSorted(List.of(message("file:/wb.xlsx?sheet=Sheet1&range=A1:B2")), model);

        var location = assertInstanceOf(TableMessageSource.class, result.getFirst().location());
        assertEquals("n1", location.id());
        assertEquals("A1", location.cell());
    }

    @Test
    void ignoresTablesOnOtherSheetsWhenLocating() {
        var otherSheet = mock(TableSyntaxNode.class);
        when(otherSheet.getUriParser()).thenReturn(new XlsUrlParser("file:/wb.xlsx?sheet=Other&range=A1:C3"));
        when(model.getAllTableSyntaxNodes()).thenReturn(Set.of(otherSheet));

        var result = mapper.mapSorted(List.of(message("file:/wb.xlsx?sheet=Sheet1&range=A1:B2")), model);

        assertNull(result.getFirst().location());
    }

    @Test
    void hasNoLocationWhenNothingMatches() {
        var result = mapper.mapSorted(List.of(message("uri")), model);

        assertEquals(1, result.size());
        assertNull(result.getFirst().location());
    }

    @Test
    void hasNoLocationForAMessageWithoutSourceLocation() {
        var result = mapper.mapSorted(List.of(message(null)), model);

        assertEquals(1, result.size());
        assertNull(result.getFirst().location());
    }

    @Test
    void aMessageRaisedInADependencyNamesThatProjectAndNotTheOneBeingCompiled() {
        var dependency = new ProjectDescriptor();
        dependency.setName("Shared Rules");
        dependency.setProjectFolder(Path.of("/workspace/design/Shared Rules"));
        var module = module("Shared", "uri");
        when(module.getProject()).thenReturn(dependency);
        var workspace = workspaceOf(module);
        when(model.getWebStudioWorkspaceDependencyManager()).thenReturn(workspace);
        var studio = mock(WebStudio.class);
        var project = mock(RulesProject.class);
        when(studio.getProjects()).thenReturn(Map.of("design", List.of(dependency)));
        when(studio.getProject("design", "Shared Rules")).thenReturn(project);
        when(model.getStudio()).thenReturn(studio);
        when(projectIdentifierMapper.map(project))
                .thenReturn(ProjectIdModel.builder().repository("design").projectName("Shared Rules").build());

        var result = mapper.mapSorted(List.of(message("uri")), model);

        // The reader is sent to the project the message was raised in, which a dependency is.
        var location = (ModuleMessageSource) result.getFirst().location();
        assertEquals("Shared", location.name());
        assertEquals("Shared Rules", location.project());
        assertEquals(ProjectIdModel.builder().repository("design").projectName("Shared Rules").build().encode(),
                location.projectId());
    }

    @Test
    void aProjectTheSessionCannotNameLeavesTheMessageWhereItIs() {
        var workspace = workspaceOf(module("Rating", "uri"));
        when(model.getWebStudioWorkspaceDependencyManager()).thenReturn(workspace);

        var result = mapper.mapSorted(List.of(message("uri")), model);

        var location = (ModuleMessageSource) result.getFirst().location();
        assertEquals("Rating", location.name());
        assertNull(location.projectId(), "a message names no project when the session cannot address one");
    }
}
