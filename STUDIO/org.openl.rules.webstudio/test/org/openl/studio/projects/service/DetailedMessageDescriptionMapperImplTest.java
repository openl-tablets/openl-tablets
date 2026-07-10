package org.openl.studio.projects.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.openl.message.OpenLMessage;
import org.openl.message.Severity;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.project.instantiation.IDependencyLoader;
import org.openl.rules.project.model.Module;
import org.openl.rules.table.xls.XlsUrlParser;
import org.openl.rules.ui.ProjectModel;
import org.openl.rules.webstudio.dependencies.WebStudioWorkspaceRelatedDependencyManager;
import org.openl.studio.projects.model.project.status.ModuleMessageSource;
import org.openl.studio.projects.model.project.status.TableMessageSource;

class DetailedMessageDescriptionMapperImplTest {

    private final DetailedMessageDescriptionMapperImpl mapper = new DetailedMessageDescriptionMapperImpl();

    private ProjectModel model;

    @BeforeEach
    void setUp() {
        model = mock(ProjectModel.class);
        when(model.getAllTableSyntaxNodes()).thenReturn(Set.of());
        when(model.getWebStudioWorkspaceDependencyManager()).thenReturn(null);
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
        var module = mock(Module.class);
        when(module.getName()).thenReturn("Rating");
        when(module.containsTable("uri")).thenReturn(true);
        var loader = mock(IDependencyLoader.class);
        when(loader.isProjectLoader()).thenReturn(false);
        when(loader.getModule()).thenReturn(module);
        var dependencyManager = mock(WebStudioWorkspaceRelatedDependencyManager.class);
        when(dependencyManager.getDependencyLoaders()).thenReturn(List.of(loader));
        when(model.getWebStudioWorkspaceDependencyManager()).thenReturn(dependencyManager);

        var result = mapper.mapSorted(List.of(message("uri")), model);

        assertEquals(1, result.size());
        var location = assertInstanceOf(ModuleMessageSource.class, result.get(0).location());
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

        assertNull(result.get(0).location());
        verify(projectLoader, times(0)).getModule();
    }

    @Test
    void resolvesAMessageToTheTableItIntersects() {
        var node = mock(TableSyntaxNode.class);
        when(node.getId()).thenReturn("n1");
        when(node.getUriParser()).thenReturn(new XlsUrlParser("file:/wb.xlsx?sheet=Sheet1&range=A1:C3"));
        when(model.getAllTableSyntaxNodes()).thenReturn(Set.of(node));

        var result = mapper.mapSorted(List.of(message("file:/wb.xlsx?sheet=Sheet1&range=A1:B2")), model);

        var location = assertInstanceOf(TableMessageSource.class, result.get(0).location());
        assertEquals("n1", location.id());
        assertEquals("A1", location.cell());
    }

    @Test
    void ignoresTablesOnOtherSheetsWhenLocating() {
        var otherSheet = mock(TableSyntaxNode.class);
        when(otherSheet.getUriParser()).thenReturn(new XlsUrlParser("file:/wb.xlsx?sheet=Other&range=A1:C3"));
        when(model.getAllTableSyntaxNodes()).thenReturn(Set.of(otherSheet));

        var result = mapper.mapSorted(List.of(message("file:/wb.xlsx?sheet=Sheet1&range=A1:B2")), model);

        assertNull(result.get(0).location());
    }

    @Test
    void hasNoLocationWhenNothingMatches() {
        var result = mapper.mapSorted(List.of(message("uri")), model);

        assertEquals(1, result.size());
        assertNull(result.get(0).location());
    }

    @Test
    void hasNoLocationForAMessageWithoutSourceLocation() {
        var result = mapper.mapSorted(List.of(message(null)), model);

        assertEquals(1, result.size());
        assertNull(result.get(0).location());
    }
}
