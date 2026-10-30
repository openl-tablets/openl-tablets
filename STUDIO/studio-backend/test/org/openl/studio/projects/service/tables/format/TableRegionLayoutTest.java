package org.openl.studio.projects.service.tables.format;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellRangeAddress;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.openl.rules.lang.xls.syntax.TableSyntaxNodeAdapter;
import org.openl.rules.table.IOpenLTable;
import org.openl.studio.projects.model.tables.RawTableGroups;
import org.openl.studio.projects.model.tables.RawTableRegion;
import org.openl.studio.projects.model.tables.RawTableRole;
import org.openl.studio.projects.model.tables.RawTableSignaturePart;
import org.openl.studio.projects.model.tables.RawTableSignaturePiece;
import org.openl.studio.projects.model.tables.RawTableValueType;
import org.openl.studio.projects.service.tables.TableTestProjects;

/**
 * Covers the regions a table is laid out in: rectangles of cells taking one role, laid out once for each compilation of
 * the table, and the types of their values.
 *
 * @author Yury Molchan
 */
class TableRegionLayoutTest {

    private static final String SHEET = "Types";

    @TempDir
    Path dir;

    @Test
    void laysADatatypeOutInAFewRegionsWhateverItsSize() throws IOException {
        var regions = regionsOf(person(3));

        assertEquals(List.of(
                new RawTableRegion(RawTableRole.SIGNATURE, null, 0, 0, 0, 2),
                new RawTableRegion(RawTableRole.CODE, null, 1, 0, 3, 0),
                new RawTableRegion(RawTableRole.OUTPUT, null, 1, 1, 3, 1),
                new RawTableRegion(RawTableRole.VALUE, RawTableValueType.STRING, 1, 2, 3, 2)), regions.regions());
        assertEquals(RawTableGroups.NONE, regions.groups());
        // A hundred times the fields: the same regions, only taller.
        var large = regionsOf(person(300)).regions();
        assertEquals(regions.regions().size(), large.size());
        assertEquals(new RawTableRegion(RawTableRole.OUTPUT, null, 1, 1, 300, 1), large.get(2));
    }

    @Test
    void namesTheTypeOfAValueOfAnotherTypeThanItsRegion() throws IOException {
        var model = TableTestProjects.projectModel(dir, SHEET, sheet -> {
            TableTestProjects.row(sheet, 1, 1, "Datatype Limits");
            TableTestProjects.row(sheet, 2, 1, "Integer", "low", "1");
            TableTestProjects.row(sheet, 3, 1, "Integer", "high", "9");
            TableTestProjects.row(sheet, 4, 1, "String", "name", "top");
        });
        var regions = regionsOf(TableTestProjects.table(model, "Limits"));

        assertEquals(new RawTableRegion(RawTableRole.VALUE, RawTableValueType.NUMBER, 1, 2, 3, 2),
                regions.regions().getLast(), "Most of the defaults are numbers");
        assertEquals(RawTableValueType.STRING, regions.typeAt(3, 2), "A text default names its own type");
        assertNull(regions.typeAt(1, 2), "A number default takes the type of its region");
        assertNull(regions.typeAt(1, 1), "A name holds no value of a type");
    }

    @Test
    void namesTheValueOfADatePropertyADate() throws IOException {
        var model = TableTestProjects.projectModel(dir, SHEET, sheet -> {
            TableTestProjects.row(sheet, 1, 1, "Rules String price(String make)");
            TableTestProjects.row(sheet, 2, 1, "properties", "effectiveDate", "01/01/2020");
            TableTestProjects.row(sheet, 3, 1, "", "state", "AR");
            // the keyword is merged over the rows of the properties, as a table is written
            sheet.addMergedRegion(new CellRangeAddress(2, 3, 1, 1));
            TableTestProjects.row(sheet, 4, 1, "C1", "RET1");
            TableTestProjects.row(sheet, 5, 1, "make == c1", "value");
            TableTestProjects.row(sheet, 6, 1, "String c1", "String value");
            TableTestProjects.row(sheet, 7, 1, "Make", "Price");
            TableTestProjects.row(sheet, 8, 1, "Toyota", "Cheap");
        });
        var regions = regionsOf(TableTestProjects.table(model, "price"));

        // the properties take no type, so the screen is told the date by the cell itself
        assertEquals(RawTableValueType.DATE, regions.typeAt(1, 2));
        assertNull(regions.typeAt(2, 2), "A property of another type is no date");
        assertNull(regions.typeAt(1, 1), "The name of a property holds no value of a type");
        assertEquals(new RawTableRegion(RawTableRole.PROPERTIES, null, 1, 0, 2, 2), regions.regions().get(1),
                "The properties stay one region");
    }

    @Test
    void namesTheValueOfADatePropertyWhoseNameIsMergedADate() throws IOException {
        var model = TableTestProjects.projectModel(dir, SHEET, sheet -> {
            TableTestProjects.row(sheet, 1, 1, "Rules String price(String make)");
            TableTestProjects.row(sheet, 2, 1, "properties", "effectiveDate", "", "01/01/2020");
            // the name of the property is merged over two columns, as an author may write it
            sheet.addMergedRegion(new CellRangeAddress(2, 2, 2, 3));
            TableTestProjects.row(sheet, 3, 1, "C1", "RET1");
            TableTestProjects.row(sheet, 4, 1, "make == c1", "value");
            TableTestProjects.row(sheet, 5, 1, "String c1", "String value");
            TableTestProjects.row(sheet, 6, 1, "Make", "Price");
            TableTestProjects.row(sheet, 7, 1, "Toyota", "Cheap");
        });
        var regions = regionsOf(TableTestProjects.table(model, "price"));

        assertEquals(RawTableValueType.DATE, regions.typeAt(1, 3), "The value after the merged name is the date");
        assertNull(regions.typeAt(1, 2), "The part of the name under the merge is no date");
    }

    @Test
    void givesTheRowsOfTheHeadersTheirRoleUpToTheEndOfTheTable() throws IOException {
        var model = TableTestProjects.projectModel(dir, SHEET, sheet -> {
            TableTestProjects.row(sheet, 1, 1, "Datatype Person");
            TableTestProjects.row(sheet, 2, 1, "String", "name");
            TableTestProjects.row(sheet, 4, 1, "Data Person people");
            TableTestProjects.merge(sheet, 4, 4, 1, 5);
            TableTestProjects.row(sheet, 5, 1, "properties", "description", "the people");
            TableTestProjects.row(sheet, 6, 1, "name");
            TableTestProjects.row(sheet, 7, 1, "Name");
            TableTestProjects.row(sheet, 8, 1, "Ann");
        });
        var roles = Roles.of(TableTestProjects.table(model, "people"));

        // The empty cells of a row of the properties, the field names and the titles take the role of the row.
        assertEquals(RawTableRole.PROPERTIES, roles.roleAt(5, 5));
        assertEquals(RawTableRole.CODE, roles.roleAt(6, 5));
        assertEquals(RawTableRole.OUTPUT_TITLE, roles.roleAt(7, 5));
    }

    @Test
    void splitsTheHeaderTextIntoItsParts() throws IOException {
        assertEquals(List.of(new RawTableSignaturePiece(RawTableSignaturePart.KEYWORD, 8),
                new RawTableSignaturePiece(RawTableSignaturePart.NAME, 15)), regionsOf(person(1)).signature());
    }

    @Test
    void keepsTheLayoutOfACompiledTableUntilItsWorkbookIsSaved() throws IOException {
        var table = person(2);
        var grid = table.getGridTable();
        var first = TableRegionLayout.regionsOf(table);

        assertSame(first, TableRegionLayout.regionsOf(table), "Every page of the table reuses its layout");
        assertNotSame(first, TableRegionLayout.regionsOf(table, grid.getRows(0, 1)),
                "A part of the table is laid out on its own");

        table.getSyntaxNode().getXlsSheetSourceCodeModule().getWorkbookSource().save();
        var saved = TableRegionLayout.regionsOf(table);
        assertNotSame(first, saved, "A workbook saved since it was compiled is laid out as it stands");
        assertEquals(first.regions(), saved.regions());
    }

    @Test
    void laysOutNoTableOfTheTypeOther() throws IOException {
        var model = TableTestProjects.projectModel(dir, SHEET,
                sheet -> TableTestProjects.row(sheet, 1, 1, "Notes about rates"));
        var other = model.getAllTableSyntaxNodes().stream()
                .map(TableSyntaxNodeAdapter::new)
                .findFirst()
                .orElseThrow();

        assertNull(TableRegionLayout.regionsOf(other));
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
