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

import org.openl.exception.OpenLCompilationException;
import org.openl.message.OpenLErrorMessage;
import org.openl.message.OpenLMessage;
import org.openl.message.OpenLWarnMessage;
import org.openl.message.Severity;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.project.instantiation.IDependencyLoader;
import org.openl.rules.project.model.Module;
import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.rules.table.ICell;
import org.openl.rules.table.IGrid;
import org.openl.rules.table.IGridTable;
import org.openl.rules.table.xls.XlsUrlParser;
import org.openl.rules.ui.ProjectModel;
import org.openl.rules.ui.WebStudio;
import org.openl.rules.webstudio.dependencies.WebStudioWorkspaceRelatedDependencyManager;
import org.openl.rules.workspace.ProjectIds;
import org.openl.source.IOpenSourceCodeModule;
import org.openl.source.impl.CompositeSourceCodeModule;
import org.openl.source.impl.StringSourceCodeModule;
import org.openl.source.impl.SubTextSourceCodeModule;
import org.openl.studio.projects.model.project.status.ModuleMessageSource;
import org.openl.studio.projects.model.project.status.TableMessageSource;
import org.openl.syntax.ISyntaxNode;
import org.openl.util.text.LocationUtils;

class DetailedMessageDescriptionMapperImplTest {

    private final DetailedMessageDescriptionMapperImpl mapper = new DetailedMessageDescriptionMapperImpl();

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

    /** The URI of a cell of column C of the workbook the tables of these tests are in. */
    private static String cellC(int row) {
        return "file:/wb.xlsx?sheet=Sheet1&cell=C" + row;
    }

    /**
     * A compilation error raised about a piece of the rule a cell at C5 holds after its '=', the way the compiler
     * reports one: the piece ends at its last character.
     */
    private static OpenLMessage errorAbout(String cellText, int from, int to) {
        var rule = new SubTextSourceCodeModule(new StringSourceCodeModule(cellText, cellC(5)), 1);
        var location = LocationUtils.createTextInterval(from, to);
        return new OpenLErrorMessage(new OpenLCompilationException("Error", null, location, rule));
    }

    /** A table of column C, holding the given texts from C5 down. */
    private TableSyntaxNode tableHolding(String... texts) {
        var grid = mock(IGrid.class);
        for (var i = 0; i < texts.length; i++) {
            var cell = mock(ICell.class);
            when(cell.getStringValue()).thenReturn(texts[i]);
            when(grid.getCell(2, 4 + i)).thenReturn(cell);
        }
        var gridTable = mock(IGridTable.class);
        when(gridTable.getGrid()).thenReturn(grid);
        var node = mock(TableSyntaxNode.class);
        when(node.getId()).thenReturn("n1");
        when(node.getGridTable()).thenReturn(gridTable);
        when(node.getUriParser())
                .thenReturn(new XlsUrlParser("file:/wb.xlsx?sheet=Sheet1&range=C5:C" + (4 + texts.length)));
        when(model.getAllTableSyntaxNodes()).thenReturn(Set.of(node));
        return node;
    }

    @Test
    void aMessageNamesThePieceOfTheCellItIsAbout() {
        tableHolding("=BlaBla+100");

        var result = mapper.mapSorted(List.of(errorAbout("=BlaBla+100", 0, 5)), model);

        // The screen draws the cell, so only where the piece begins and ends crosses the wire — counted in the
        // cell's own text, which the '=' of a spreadsheet step begins one character before the rule.
        var location = assertInstanceOf(TableMessageSource.class, result.getFirst().location());
        assertEquals(1, location.start());
        assertEquals(7, location.end());
    }

    @Test
    void aMessageAboutAWholeRuleMarksAllOfItInTheCell() {
        tableHolding("=BlaBla+100");
        var rule = new SubTextSourceCodeModule(new StringSourceCodeModule("=BlaBla+100", cellC(5)), 1);
        var error = new OpenLCompilationException("Error", null, null, rule);

        var result = mapper.mapSorted(List.of(new OpenLErrorMessage(error)), model);

        var location = (TableMessageSource) result.getFirst().location();
        assertEquals(1, location.start());
        assertEquals(11, location.end());
    }

    @Test
    void aWarningNamesThePieceOfTheCellItWasRaisedAbout() {
        tableHolding("Bonus > limit");
        var node = mock(ISyntaxNode.class);
        when(node.getModule()).thenReturn(new StringSourceCodeModule("Bonus > limit", cellC(5)));
        when(node.getSourceLocation()).thenReturn(LocationUtils.createTextInterval(0, 4));
        var warning = new OpenLWarnMessage("deprecated", node);

        var result = mapper.mapSorted(List.of(warning), model);

        var location = (TableMessageSource) result.getFirst().location();
        assertEquals(0, location.start());
        assertEquals(5, location.end());
    }

    @Test
    void aMessageTheCellNoLongerSpellsOutMarksNothing() {
        tableHolding("=Something else");

        var result = mapper.mapSorted(List.of(errorAbout("=BlaBla+100", 0, 5)), model);

        var location = (TableMessageSource) result.getFirst().location();
        assertNull(location.start(), "a rule that is not what the cell says has nothing to mark in it");
    }

    @Test
    void aMessageInAMethodBodyMarksThePieceInTheCellThatHoldsIt() {
        tableHolding("int a = 1;\nint b = 2;", "return format(a);");
        var body = new CompositeSourceCodeModule(new IOpenSourceCodeModule[]{
                new StringSourceCodeModule("int a = 1;\nint b = 2;", cellC(5)),
                new StringSourceCodeModule("return format(a);", cellC(6))}, "\n");
        // "format(a)" on the third line of the body, which is the first line of the second cell.
        var error = new OpenLCompilationException("Error", null, LocationUtils.createTextInterval(29, 37), body);

        var result = mapper.mapSorted(List.of(new OpenLErrorMessage(error)), model);

        var location = (TableMessageSource) result.getFirst().location();
        assertEquals("C6", location.cell());
        assertEquals(7, location.start());
        assertEquals(16, location.end());
    }

    @Test
    void aMessageAboutABlankCellMarksNothing() {
        tableHolding("   ");
        var error = new OpenLCompilationException("Error", null, null, new StringSourceCodeModule("   ", cellC(5)));

        var result = mapper.mapSorted(List.of(new OpenLErrorMessage(error)), model);

        var location = (TableMessageSource) result.getFirst().location();
        assertNull(location.start());
        assertNull(location.end());
    }

    @Test
    void aMessageAboutTheTableItselfMarksNothing() {
        var node = tableHolding("=BlaBla+100");
        when(node.getUriParser()).thenReturn(new XlsUrlParser("file:/wb.xlsx?sheet=Sheet1&range=C5:D6"));

        var result = mapper.mapSorted(List.of(message("file:/wb.xlsx?sheet=Sheet1&range=C5:D6")), model);

        var location = (TableMessageSource) result.getFirst().location();
        assertNull(location.start());
        assertNull(location.end());
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
        when(project.getProjectId()).thenReturn(ProjectIds.of("design", "Shared Rules"));

        var result = mapper.mapSorted(List.of(message("uri")), model);

        // The reader is sent to the project the message was raised in, which a dependency is.
        var location = (ModuleMessageSource) result.getFirst().location();
        assertEquals("Shared", location.name());
        assertEquals("Shared Rules", location.project());
        assertEquals(ProjectIds.of("design", "Shared Rules").value(), location.projectId());
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
