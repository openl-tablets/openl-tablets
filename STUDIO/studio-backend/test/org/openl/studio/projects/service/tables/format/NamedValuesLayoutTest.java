package org.openl.studio.projects.service.tables.format;

import static org.junit.jupiter.api.Assertions.assertEquals;

import static org.openl.studio.projects.service.tables.TableTestProjects.merge;

import java.io.IOException;
import java.nio.file.Path;

import org.apache.poi.ss.usermodel.Sheet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.openl.rules.table.IOpenLTable;
import org.openl.studio.projects.model.tables.RawTableRole;
import org.openl.studio.projects.model.tables.RawTableValueType;
import org.openl.studio.projects.service.tables.TableTestProjects;

/**
 * Covers the roles of the tables that name a value in each row: an Environment, a Properties and a Constants table,
 * written either way round.
 */
class NamedValuesLayoutTest {

    private static final String SHEET = "Settings";

    /** An Environment table importing two packages, the setting merged over both. */
    private static final int ENVIRONMENT_ROW = 1;

    /** A Properties table of a category of tables. */
    private static final int PROPERTIES_ROW = 6;

    /** A Constants table, a constant in each row. */
    private static final int CONSTANTS_ROW = 11;

    /** A Constants table written transposed, a constant in each column. */
    private static final int TRANSPOSED_ROW = 16;

    /** A Constants table whose first row reads as the titles of a Datatype, which the compiler reads as a constant. */
    private static final int TITLED_ROW = 22;

    @TempDir
    Path dir;

    @BeforeEach
    void writeProject() throws IOException {
        TableTestProjects.projectModel(dir, SHEET, NamedValuesLayoutTest::fillSheet);
    }

    @Test
    void readsTheRolesOfTheTablesNamingAValueInEachRow() {
        var environment = Roles.of(tableAt(ENVIRONMENT_ROW));
        assertEquals(RawTableRole.KEY, environment.roleAt(ENVIRONMENT_ROW + 2, 1), "A merged setting");
        assertEquals(RawTableRole.VALUE, environment.roleAt(ENVIRONMENT_ROW + 2, 2));

        var properties = Roles.of(tableAt(PROPERTIES_ROW));
        assertEquals(RawTableRole.KEY, properties.roleAt(PROPERTIES_ROW + 1, 1));
        assertEquals(RawTableRole.VALUE, properties.roleAt(PROPERTIES_ROW + 2, 2));

        var constants = Roles.of(tableAt(CONSTANTS_ROW));
        assertEquals(RawTableRole.CODE, constants.roleAt(CONSTANTS_ROW + 1, 1), "The type of a constant");
        assertEquals(RawTableRole.KEY, constants.roleAt(CONSTANTS_ROW + 1, 2), "Its name");
        assertEquals(RawTableRole.VALUE, constants.roleAt(CONSTANTS_ROW + 2, 3), "Its value");
        assertEquals(RawTableValueType.NUMBER, constants.typeAt(CONSTANTS_ROW + 2, 3));

        var transposed = Roles.of(tableAt(TRANSPOSED_ROW));
        assertEquals(RawTableRole.CODE, transposed.roleAt(TRANSPOSED_ROW + 1, 2));
        assertEquals(RawTableRole.KEY, transposed.roleAt(TRANSPOSED_ROW + 2, 2));
        assertEquals(RawTableRole.VALUE, transposed.roleAt(TRANSPOSED_ROW + 3, 1));

        // The compiler reads every row as a constant, its type first: a row naming the titles of a Datatype too.
        var titled = Roles.of(tableAt(TITLED_ROW));
        for (var row = TITLED_ROW + 1; row <= TITLED_ROW + 2; row++) {
            assertEquals(RawTableRole.CODE, titled.roleAt(row, 1), "row " + row);
            assertEquals(RawTableRole.KEY, titled.roleAt(row, 2), "row " + row);
            assertEquals(RawTableRole.VALUE, titled.roleAt(row, 3), "row " + row);
        }
    }

    /** The table whose header stands in the given row: an Environment table has no name to find it by. */
    private IOpenLTable tableAt(int row) {
        return TableTestProjects.tableAt(dir, row);
    }

    /**
     * An Environment, a Properties and a Constants table, a Constants table written transposed, and one whose first row
     * reads as the titles of a Datatype.
     */
    private static void fillSheet(Sheet sheet) {
        TableTestProjects.row(sheet, ENVIRONMENT_ROW, 1, "Environment");
        merge(sheet, ENVIRONMENT_ROW, ENVIRONMENT_ROW, 1, 2);
        TableTestProjects.row(sheet, ENVIRONMENT_ROW + 1, 1, "import", "java.util");
        TableTestProjects.row(sheet, ENVIRONMENT_ROW + 2, 1, null, "java.time");
        merge(sheet, ENVIRONMENT_ROW + 1, ENVIRONMENT_ROW + 2, 1, 1);

        TableTestProjects.row(sheet, PROPERTIES_ROW, 1, "Properties Catalogue");
        merge(sheet, PROPERTIES_ROW, PROPERTIES_ROW, 1, 2);
        TableTestProjects.row(sheet, PROPERTIES_ROW + 1, 1, "scope", "Category");
        TableTestProjects.row(sheet, PROPERTIES_ROW + 2, 1, "category", "Catalogue");

        TableTestProjects.row(sheet, CONSTANTS_ROW, 1, "Constants Limits");
        merge(sheet, CONSTANTS_ROW, CONSTANTS_ROW, 1, 3);
        TableTestProjects.row(sheet, CONSTANTS_ROW + 1, 1, "Integer", "DEFAULT_AGE", "43");
        TableTestProjects.row(sheet, CONSTANTS_ROW + 2, 1, "Double", "MAX_RATE", "0.7");

        TableTestProjects.row(sheet, TRANSPOSED_ROW, 1, "Constants Bounds");
        merge(sheet, TRANSPOSED_ROW, TRANSPOSED_ROW, 1, 2);
        TableTestProjects.row(sheet, TRANSPOSED_ROW + 1, 1, "Integer", "Double");
        TableTestProjects.row(sheet, TRANSPOSED_ROW + 2, 1, "MIN_AGE", "MIN_RATE");
        TableTestProjects.row(sheet, TRANSPOSED_ROW + 3, 1, "18", "0.1");

        TableTestProjects.row(sheet, TITLED_ROW, 1, "Constants Titled");
        merge(sheet, TITLED_ROW, TITLED_ROW, 1, 3);
        TableTestProjects.row(sheet, TITLED_ROW + 1, 1, "Name", "Type", "Value");
        TableTestProjects.row(sheet, TITLED_ROW + 2, 1, "Integer", "LIMIT", "10");
    }
}
