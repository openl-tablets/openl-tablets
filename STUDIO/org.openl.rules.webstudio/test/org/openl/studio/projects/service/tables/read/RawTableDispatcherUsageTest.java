package org.openl.studio.projects.service.tables.read;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.openl.rules.lang.xls.syntax.TableUtils;
import org.openl.rules.ui.ProjectModel;
import org.openl.studio.projects.model.tables.RawTableView;
import org.openl.studio.projects.service.tables.TableModules;
import org.openl.studio.projects.service.tables.TableTestProjects;

/**
 * Verifies that a word naming a rule written in several versions leads to the table that chooses between them.
 *
 * <p>The module of the fixture writes {@code Hello} twice, for two states, and calls it from {@code CallHello}.
 * The compiler builds a table of its own to choose between the versions. It sits in no workbook, yet the module
 * holds it, so the word in {@code CallHello} opens it through that module, as OpenL Studio always did.
 */
class RawTableDispatcherUsageTest {

    private static final String CALL = "return Hello(\"x\");";

    private ProjectModel projectModel;

    @BeforeEach
    void writeModule(@TempDir Path dir) throws IOException {
        projectModel = TableTestProjects.projectModel(dir, "Rules", sheet -> {
            TableTestProjects.helloInTwoVersions(sheet);
            TableTestProjects.row(sheet, 13, 1, "Method String CallHello()");
            TableTestProjects.row(sheet, 14, 1, CALL);
        });
    }

    @Test
    void leadsToTheTableChoosingBetweenTheVersions() {
        var read = readCaller();

        var usage = TableTestProjects.cellOf(read.source, CALL).metaInfo().usages().getFirst();
        var dispatcher = TableTestProjects.dispatcherTable(projectModel);
        assertEquals(TableUtils.makeTableId(dispatcher.getUri()), usage.tableId());
        // It is read through the module the word is read in: no workbook holds it to name another one.
        assertEquals(projectModel.getModuleInfo().getName(), usage.module());
        assertNotNull(usage.description());
    }

    @Test
    void holdsTheTableItsCompilationBuilt() {
        // The table is built once something asks where a word leads: the screen reads the caller before the
        // reader follows the word.
        readCaller();
        var dispatcher = TableTestProjects.dispatcherTable(projectModel);

        assertTrue(projectModel.isGeneratedTable(dispatcher));
        assertTrue(projectModel.holdsTable(dispatcher.getUri()), "read through the module that built it");
        // The versions themselves are written in the workbook: held, but not built by the compiler.
        var version = TableTestProjects.table(projectModel, "Hello");
        assertTrue(projectModel.holdsTable(version.getUri()));
        assertFalse(projectModel.isGeneratedTable(version));
        assertFalse(projectModel.holdsTable(null));
        assertFalse(projectModel.holdsTable("file:/elsewhere/Other.xlsx?sheet=Rules&range=A1:B2"));
    }

    /** Reads {@code CallHello} the way the screen does, with what the compiler knows about its cells. */
    private RawTableView readCaller() {
        return new RawTableReader().read(TableTestProjects.table(projectModel, "CallHello"), RawTableRead.builder()
                .withMetaInfo(true)
                .modules(TableModules.ofWorkspace(projectModel))
                .build());
    }
}
