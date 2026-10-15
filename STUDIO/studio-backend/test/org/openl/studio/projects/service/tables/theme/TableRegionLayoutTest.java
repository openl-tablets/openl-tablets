package org.openl.studio.projects.service.tables.theme;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import org.apache.poi.ss.usermodel.Sheet;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.openl.rules.lang.xls.syntax.TableSyntaxNodeAdapter;
import org.openl.rules.table.IOpenLTable;
import org.openl.studio.projects.service.tables.TableTestProjects;
import org.openl.studio.projects.service.tables.theme.TableRegions.HeaderPart;
import org.openl.studio.projects.service.tables.theme.TableRegions.HeaderPiece;
import org.openl.studio.projects.service.tables.theme.TableRegions.Region;

/**
 * Covers the regions a table is laid out in: rectangles of cells naming the same parts of a theme, laid out once for
 * each compilation of the table.
 *
 * @author Yury Molchan
 */
class TableRegionLayoutTest {

    private static final String SHEET = "Types";

    private static final Parts HEADER = Parts.BASE.with(Parts.Part.HEADER);
    private static final Parts TYPE = Parts.BASE.with(Parts.Part.TYPE);
    private static final Parts NAME = Parts.BASE.with(Parts.Part.NAME);
    private static final Parts VALUES = Parts.BASE.with(Parts.Part.VALUES);

    @TempDir
    Path dir;

    @Test
    void laysADatatypeOutInAFewRegionsWhateverItsSize() throws IOException {
        var regions = regionsOf(person(3));

        assertEquals(List.of(
                new Region(HEADER, 0, 0, 0, 2),
                new Region(TYPE, 1, 0, 2, 0),
                new Region(NAME, 1, 1, 2, 1),
                new Region(VALUES, 1, 2, 2, 2),
                new Region(TYPE.with(Parts.Part.LAST_ROW), 3, 0, 3, 0),
                new Region(NAME.with(Parts.Part.LAST_ROW), 3, 1, 3, 1),
                new Region(VALUES.with(Parts.Part.LAST_ROW), 3, 2, 3, 2)), regions.regions());
        // A hundred times the fields: the same regions, only taller.
        var large = regionsOf(person(300)).regions();
        assertEquals(regions.regions().size(), large.size());
        assertEquals(new Region(NAME, 1, 1, 299, 1), large.get(2));
    }

    @Test
    void splitsTheHeaderTextIntoItsParts() throws IOException {
        assertEquals(List.of(new HeaderPiece(HeaderPart.KEYWORD, 0, 8),
                new HeaderPiece(HeaderPart.TEXT, 8, 9),
                new HeaderPiece(HeaderPart.NAME, 9, 15)), regionsOf(person(1)).signature());
    }

    @Test
    void keepsTheLayoutOfACompiledTableUntilItsWorkbookIsSaved() throws IOException {
        var table = person(2);
        var grid = table.getGridTable();
        var first = TableRegionLayout.regionsOf(table, grid);

        assertSame(first, TableRegionLayout.regionsOf(table, grid), "Every page of the table reuses its layout");
        assertNotSame(first, TableRegionLayout.regionsOf(table, grid.getRows(0, 1)),
                "A part of the table is laid out on its own");

        table.getSyntaxNode().getXlsSheetSourceCodeModule().getWorkbookSource().save();
        var saved = TableRegionLayout.regionsOf(table, grid);
        assertNotSame(first, saved, "A workbook saved since it was compiled is laid out as it stands");
        assertEquals(first, saved);
    }

    @Test
    void laysOutNoTableOfAKindNoThemeStyles() throws IOException {
        var model = TableTestProjects.projectModel(dir, SHEET,
                sheet -> TableTestProjects.row(sheet, 1, 1, "Notes about rates"));
        var other = model.getAllTableSyntaxNodes().stream()
                .map(TableSyntaxNodeAdapter::new)
                .findFirst()
                .orElseThrow();

        assertNull(TableRegionLayout.regionsOf(other, other.getGridTable()));
    }

    /** The regions of a table, laid out afresh. */
    private static TableRegions regionsOf(IOpenLTable table) {
        return TableRegionLayout.of(table, table.getGridTable());
    }

    /** A Datatype of as many fields as asked for, each a type, a name and a default value. */
    private IOpenLTable person(int fields) throws IOException {
        var model = TableTestProjects.projectModel(dir, SHEET, sheet -> writePerson(sheet, fields));
        return TableTestProjects.table(model, "Person");
    }

    private static void writePerson(Sheet sheet, int fields) {
        TableTestProjects.row(sheet, 1, 1, "Datatype Person");
        for (var field = 0; field < fields; field++) {
            TableTestProjects.row(sheet, field + 2, 1, "String", "field" + field, "value" + field);
        }
    }
}
