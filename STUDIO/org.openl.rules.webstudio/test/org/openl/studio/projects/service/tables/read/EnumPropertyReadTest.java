package org.openl.studio.projects.service.tables.read;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.openl.rules.enumeration.ValidateDTEnum;
import org.openl.rules.lang.xls.syntax.TableSyntaxNodeAdapter;
import org.openl.rules.table.IOpenLTable;
import org.openl.rules.ui.ProjectModel;
import org.openl.studio.projects.model.tables.RawTableCell;
import org.openl.studio.projects.service.tables.TableModules;
import org.openl.studio.projects.service.tables.TableTestProjects;

/**
 * Confirms a table opens when a property of it holds an enumeration's value written the way the engine reads it.
 *
 * <p>The module of the fixture is written the way older workbooks write properties: a Properties table holding
 * {@code validateDT = on}, and a table declaring {@code validateDT = off} in a properties section of its own.
 * The engine reads either cell, so the module compiles and the value takes part in the rules.
 *
 * <p>A cell naming no value of its property at all is drawn as the text it holds, so a workbook someone
 * uploaded with a typo in it opens and can be corrected.
 */
class EnumPropertyReadTest {

    private ProjectModel projectModel;

    @BeforeEach
    void writeModule(@TempDir Path tempDir) throws Exception {
        var project = TableTestProjects.writeProject(tempDir.resolve("props"), "Props", "Rules", new String[][]{
                {"Properties", null, null},
                {"validateDT", "on", null},
                {null, null, null},
                {"Method String hello(int hour)", null, null},
                {"properties", "validateDT", "off"},
                {"hello", null, null},
                {"= \"Hi\"", null, null},
        });
        projectModel = TableTestProjects.projectModel(project);
    }

    @Test
    void readsAPropertyWrittenInTheCaseItsAuthorUsed() {
        assertEquals(ValidateDTEnum.ON, valueBeside("validateDT", table(projectModel, "xls.properties")));
        assertEquals(ValidateDTEnum.OFF, valueBeside("validateDT", table(projectModel, "xls.method")));
    }

    @Test
    void keepsTheTextOfACellNamingNoValueSoTheTableStillOpens(@TempDir Path tempDir) throws Exception {
        // A workbook can hold anything an author typed. A cell naming no value of the property is theirs to
        // correct, so it is drawn as it stands rather than refused.
        var project = TableTestProjects.writeProject(tempDir.resolve("typo"), "Typo", "Rules", new String[][]{
                {"Properties", null},
                {"validateDT", "mabye"},
        });
        var typo = TableTestProjects.projectModel(project);

        assertEquals("mabye", valueBeside("validateDT", table(typo, "xls.properties")));
    }

    @Test
    void drawsEveryTableOfSuchAModule() {
        for (var tsn : projectModel.getAllTableSyntaxNodes()) {
            var read = read(new TableSyntaxNodeAdapter(tsn));
            assertTrue(read.size() > 1, "a table is drawn with the rows under its header");
        }
    }

    /** The value read out of the cell that follows the named one. */
    private static Object valueBeside(String name, IOpenLTable table) {
        var cells = read(table).stream().flatMap(List::stream).toList();
        for (var at = 0; at < cells.size() - 1; at++) {
            if (name.equals(cells.get(at).value())) {
                return cells.get(at + 1).value();
            }
        }
        throw new IllegalStateException("No cell reading '" + name + "'");
    }

    /** The table as the table screen reads it: every cell with its style and what the compiler knows about it. */
    private static List<List<RawTableCell>> read(IOpenLTable table) {
        return new RawTableReader().read(table, null, null, true, true, TableModules.none()).source;
    }

    /** The one table with the given type of the given module. */
    private static IOpenLTable table(ProjectModel model, String type) {
        for (var tsn : model.getAllTableSyntaxNodes()) {
            if (type.equals(tsn.getType())) {
                return new TableSyntaxNodeAdapter(tsn);
            }
        }
        throw new IllegalStateException("No " + type + " table");
    }
}
