package org.openl.studio.projects.service.tables.format;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Path;
import java.util.List;

import org.apache.poi.ss.usermodel.Sheet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.openl.rules.ui.ProjectModel;
import org.openl.studio.projects.service.tables.TableTestProjects;

/** The lines that set apart the rows of a Data and a Test table taking several lines each. */
class DataLayoutTest {

    private ProjectModel model;

    @BeforeEach
    void writeProject(@TempDir Path dir) throws Exception {
        model = TableTestProjects.projectModel(dir, "Rules", DataLayoutTest::fillSheet);
    }

    @Test
    void setsApartTheRowsOfADataTableWhenARowTakesSeveralLines() {
        var groups = Roles.of(TableTestProjects.table(model, "policies")).regions().groups();

        // The second policy begins five rows below the header: a line sets it apart from the first one.
        assertEquals(List.of(5), groups.rows());
        assertEquals(List.of(), groups.columns());
    }

    @Test
    void drawsNoLineBetweenTheRowsOfADataTableTakingALineEach() {
        var groups = Roles.of(TableTestProjects.table(model, "flatPolicies")).regions().groups();

        assertEquals(List.of(), groups.rows());
        assertEquals(List.of(), groups.columns());
    }

    @Test
    void setsApartTheCasesOfATransposedTestTableOnTheLeftOfEachCase() {
        var groups = Roles.of(TableTestProjects.table(model, "countTest")).regions().groups();

        // The second case begins in the fourth column of the table, after the titles and the two columns of the first.
        assertEquals(List.of(), groups.rows());
        assertEquals(List.of(4), groups.columns());
    }

    private static void fillSheet(Sheet sheet) {
        TableTestProjects.row(sheet, 1, 1, "Datatype Vehicle");
        TableTestProjects.row(sheet, 2, 1, "String", "name");
        TableTestProjects.row(sheet, 4, 1, "Datatype Policy");
        TableTestProjects.row(sheet, 5, 1, "String", "name");
        TableTestProjects.row(sheet, 6, 1, "Vehicle[]", "vehicles");

        // The first policy holds two vehicles, one per row, its name merged over them.
        TableTestProjects.row(sheet, 8, 1, "Data Policy policies");
        TableTestProjects.row(sheet, 9, 1, "name", "vehicles.name");
        TableTestProjects.row(sheet, 10, 1, "Policy", "Vehicle");
        TableTestProjects.row(sheet, 11, 1, "P1", "A");
        TableTestProjects.row(sheet, 12, 2, "B");
        TableTestProjects.row(sheet, 13, 1, "P2", "C");
        TableTestProjects.merge(sheet, 11, 12, 1, 1);

        TableTestProjects.row(sheet, 15, 1, "Data Policy flatPolicies");
        TableTestProjects.row(sheet, 16, 1, "name", "vehicles.name");
        TableTestProjects.row(sheet, 17, 1, "Policy", "Vehicle");
        TableTestProjects.row(sheet, 18, 1, "P3", "D");
        TableTestProjects.row(sheet, 19, 1, "P4", "E");

        TableTestProjects.row(sheet, 21, 1, "Method Integer count(Policy pol)");
        TableTestProjects.row(sheet, 22, 1, "return length(pol.vehicles);");

        // The same policies tested the other way round: the first case takes two columns.
        TableTestProjects.row(sheet, 24, 1, "Test count countTest");
        TableTestProjects.row(sheet, 25, 1, "pol.name", "Policy", "P1", "", "P2");
        TableTestProjects.row(sheet, 26, 1, "pol.vehicles.name", "Vehicle", "A", "B", "C");
        TableTestProjects.row(sheet, 27, 1, "_res_", "Count", "2", "", "1");
        TableTestProjects.merge(sheet, 25, 25, 3, 4);
        TableTestProjects.merge(sheet, 27, 27, 3, 4);
    }
}
