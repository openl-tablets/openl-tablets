package org.openl.studio.projects.service.tables;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.function.Consumer;

import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import org.openl.rules.project.instantiation.ReloadType;
import org.openl.rules.project.model.Module;
import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.rules.table.IOpenLTable;
import org.openl.rules.ui.ProjectModel;
import org.openl.studio.common.exception.GoneException;

class StaleTableIdsTest {

    private static final String TABLES = "/rest/projects/cHJvamVjdA/tables/";

    @TempDir
    private Path project;

    @AfterEach
    void unbindRequest() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void aRenamedTableIsServedUnderTheIdItHasNow() {
        var renamed = mock(IOpenLTable.class);
        var model = model(Map.of("b", renamed));
        var response = request(TABLES + "a/tests", null);

        StaleTableIds.renamed(project, "a", "b");

        assertSame(renamed, StaleTableIds.tableOf(model, "a"));
        assertEquals(TABLES + "b/tests", response.getHeader(HttpHeaders.CONTENT_LOCATION));
    }

    @Test
    void aChainOfRenamesLeadsToItsEnd() {
        var renamed = mock(IOpenLTable.class);
        var model = model(Map.of("c", renamed));

        StaleTableIds.renamed(project, "a", "b");
        StaleTableIds.renamed(project, "b", "c");

        assertSame(renamed, StaleTableIds.tableOf(model, "a"));
        assertSame(renamed, StaleTableIds.tableOf(model, "b"));
    }

    @Test
    void anIdATableHasNowNamesThatTable() {
        var live = mock(IOpenLTable.class);
        var model = model(Map.of("a", live, "b", mock(IOpenLTable.class)));
        var response = request(TABLES + "a", null);

        StaleTableIds.renamed(project, "a", "b");

        assertSame(live, StaleTableIds.tableOf(model, "a"));
        assertNull(response.getHeader(HttpHeaders.CONTENT_LOCATION));
    }

    @Test
    void aDeletedTableIsGoneAndSoAreTheIdsThatLedToIt() {
        var model = model(Map.of());

        StaleTableIds.renamed(project, "a", "b");
        StaleTableIds.deleted(project, "b");

        assertThrows(GoneException.class, () -> StaleTableIds.tableOf(model, "a"));
        assertThrows(GoneException.class, () -> StaleTableIds.tableOf(model, "b"));
    }

    @Test
    void aDeletedTableStaysDeletedThoughATableLaterStandsWhereItStood() throws Exception {
        var model = TableTestProjects.projectModel(project, "Main", sheet -> {
            TableTestProjects.row(sheet, 1, 1, "Datatype Greeting");
            TableTestProjects.row(sheet, 2, 1, "String", "text");
        });
        var greeting = TableTestProjects.table(model, "Greeting").getId();
        StaleTableIds.deleted(project, greeting);
        workbook(sheet -> {
            TableTestProjects.row(sheet, 1, 1, "Datatype Salute");
            TableTestProjects.row(sheet, 2, 1, "String", "text");
        });

        model.reset(ReloadType.RELOAD);

        TableTestProjects.table(model, "Salute");
        assertThrows(GoneException.class, () -> StaleTableIds.tableOf(model, greeting));
    }

    @Test
    void aStudioRenameOfATableThatTookADeletedIdAgainIsLearned() {
        var renamed = mock(IOpenLTable.class);
        var model = model(Map.of("b", renamed));

        StaleTableIds.deleted(project, "a");
        StaleTableIds.renamed(project, "a", "b");

        assertSame(renamed, StaleTableIds.tableOf(model, "a"));
    }

    @Test
    void anIdATableTakesAgainLeadsNowhereElse() {
        var model = model(Map.of("b", mock(IOpenLTable.class)));

        StaleTableIds.renamed(project, "a", "b");
        StaleTableIds.renamed(project, "c", "a");
        StaleTableIds.deleted(project, "d");
        StaleTableIds.renamed(project, "e", "d");

        assertNull(StaleTableIds.tableOf(model, "a"), "the table that took the id back may be gone unannounced");
        assertEquals("d", StaleTableIds.currentIdOf(project, "d"));
    }

    @Test
    void anIdTheLedgerKnowsNothingOfIsNotFound() {
        assertNull(StaleTableIds.tableOf(model(Map.of()), "a"));
        assertEquals("a", StaleTableIds.currentIdOf(project, "a"));
        assertNull(StaleTableIds.tableOf(mock(ProjectModel.class), "a"), "a model of no project");
    }

    @Test
    void eachProjectHasALedgerOfItsOwn() {
        StaleTableIds.renamed(project, "a", "b");

        assertEquals("a", StaleTableIds.currentIdOf(project.resolve("other"), "a"));
        assertEquals("b", StaleTableIds.currentIdOf(project.resolve("x").resolve(".."), "a"));
    }

    @Test
    void onlyTheMostRecentChangesAreRemembered() {
        for (var i = 0; i <= 1000; i++) {
            StaleTableIds.renamed(project, "old" + i, "new" + i);
        }

        assertEquals("old0", StaleTableIds.currentIdOf(project, "old0"));
        assertEquals("new1", StaleTableIds.currentIdOf(project, "old1"));
        assertEquals("new1000", StaleTableIds.currentIdOf(project, "old1000"));
    }

    @Test
    void theIdOfAQueryNamesTheTableAsTheAddressDoes() {
        var response = request("/rest/projects/cHJvamVjdA/run", "tableId=a&fromModule=a");

        StaleTableIds.renamed(project, "a", "b");

        assertEquals("b", StaleTableIds.currentIdOf(project, "a"));
        assertEquals("/rest/projects/cHJvamVjdA/run?tableId=b&fromModule=a",
                response.getHeader(HttpHeaders.CONTENT_LOCATION));
    }

    @Test
    void aTableRenamedInExcelIsKnownByItsOldIdAfterTheProjectIsCompiledAgain() throws Exception {
        var model = TableTestProjects.projectModel(project, "Main", sheet -> {
            TableTestProjects.row(sheet, 1, 1, "Datatype Greeting");
            TableTestProjects.row(sheet, 2, 1, "String", "text");
            TableTestProjects.row(sheet, 4, 1, "Datatype Farewell");
            TableTestProjects.row(sheet, 5, 1, "String", "text");
        });
        var greeting = TableTestProjects.table(model, "Greeting").getId();
        var farewell = TableTestProjects.table(model, "Farewell").getId();
        // Renamed outside OpenL Studio, the other table moved down a row.
        workbook(sheet -> {
            TableTestProjects.row(sheet, 1, 1, "Datatype Salute");
            TableTestProjects.row(sheet, 2, 1, "String", "text");
            TableTestProjects.row(sheet, 5, 1, "Datatype Farewell");
            TableTestProjects.row(sheet, 6, 1, "String", "text");
        });

        model.reset(ReloadType.RELOAD);

        var salute = TableTestProjects.table(model, "Salute");
        assertNotEquals(greeting, salute.getId());
        assertEquals(salute.getUri(), StaleTableIds.tableOf(model, greeting).getUri());
        assertEquals(farewell, TableTestProjects.table(model, "Farewell").getId());
    }

    @Test
    void aCompilationWhoseIdsNobodyAskedForTeachesNothing() throws Exception {
        var model = TableTestProjects.projectModel(project, "Main", sheet -> {
            TableTestProjects.row(sheet, 1, 1, "Datatype Greeting");
            TableTestProjects.row(sheet, 2, 1, "String", "text");
        });
        var compilations = new StaleTableIds.Compilations();
        compilations.dropped(model.getXlsModuleNode());
        var greeting = TableTestProjects.table(model, "Greeting").getId();
        workbook(sheet -> {
            TableTestProjects.row(sheet, 1, 1, "Datatype Salute");
            TableTestProjects.row(sheet, 2, 1, "String", "text");
        });

        compilations.compiled(TableTestProjects.projectModel(project).getXlsModuleNode());

        assertEquals(greeting, StaleTableIds.currentIdOf(project, greeting));
    }

    private ProjectModel model(Map<String, IOpenLTable> tables) {
        var model = mock(ProjectModel.class);
        tables.forEach((id, table) -> when(model.getTableById(id)).thenReturn(table));
        var module = mock(Module.class);
        var descriptor = mock(ProjectDescriptor.class);
        when(model.getModuleInfo()).thenReturn(module);
        when(module.getProject()).thenReturn(descriptor);
        when(descriptor.getProjectFolder()).thenReturn(project);
        return model;
    }

    private static MockHttpServletResponse request(String uri, String query) {
        var request = new MockHttpServletRequest("GET", uri);
        request.setQueryString(query);
        var response = new MockHttpServletResponse();
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request, response));
        return response;
    }

    private void workbook(Consumer<Sheet> content) throws IOException {
        try (var workbook = new XSSFWorkbook()) {
            content.accept(workbook.createSheet("Main"));
            try (OutputStream out = Files.newOutputStream(project.resolve("Main.xlsx"))) {
                workbook.write(out);
            }
        }
    }
}
