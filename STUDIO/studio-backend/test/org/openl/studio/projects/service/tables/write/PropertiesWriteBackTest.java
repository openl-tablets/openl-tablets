package org.openl.studio.projects.service.tables.write;

import static org.junit.jupiter.api.Assertions.assertEquals;

import static org.openl.studio.projects.service.tables.TableTestProjects.merge;
import static org.openl.studio.projects.service.tables.TableTestProjects.row;

import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedHashMap;

import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.openl.rules.enumeration.UsStatesEnum;
import org.openl.studio.projects.model.tables.SimpleRulesView;
import org.openl.studio.projects.service.tables.TableTestProjects;
import org.openl.studio.projects.service.tables.read.SimpleRulesTableReader;

/**
 * A table written back as it was read keeps its properties, a property of several values included.
 *
 * @author Yury Molchan
 */
class PropertiesWriteBackTest {

    @TempDir
    Path tempDir;

    @Test
    void keepsAPropertyOfSeveralValuesWrittenBackAsRead() throws Exception {
        var project = project();

        writeBack(project);
        writeBack(project);

        try (var workbook = new XSSFWorkbook(Files.newInputStream(project.resolve("props.xlsx")))) {
            var sheet = workbook.getSheet("Rules");
            // a blank element stays blank: the engine reads it last, and it is never written as the text null
            assertEquals("Auto\\,Special,Home,", sheet.getRow(1).getCell(2).getStringCellValue());
            assertEquals("CA,NY", sheet.getRow(2).getCell(2).getStringCellValue());
        }
        var properties = read(project).properties;
        assertEquals(Arrays.asList("Auto,Special", "Home", null), Arrays.asList((Object[]) properties.get("lob")));
        assertEquals(Arrays.asList(UsStatesEnum.CA, UsStatesEnum.NY), Arrays.asList((Object[]) properties.get("state")));
    }

    /** {@code myRule} with the properties {@code lob = "Auto,Special", (blank), Home} and {@code state = CA, NY}. */
    private Path project() throws Exception {
        var dir = Files.createDirectories(tempDir.resolve("props"));
        try (var workbook = new XSSFWorkbook()) {
            var sheet = workbook.createSheet("Rules");
            row(sheet, 0, 0, "SimpleRules String myRule(int a)");
            row(sheet, 1, 0, "properties", "lob", "Auto\\,Special,,Home");
            row(sheet, 2, 0, "", "state", "CA,NY");
            row(sheet, 3, 0, "a", "res");
            row(sheet, 4, 0, "1", "one");
            merge(sheet, 1, 2, 0, 0);
            try (OutputStream out = Files.newOutputStream(dir.resolve("props.xlsx"))) {
                workbook.write(out);
            }
        }
        return dir;
    }

    /** Writes the table back with what a read of it gives, its properties as the view lists them. */
    private static void writeBack(Path project) {
        var table = TableTestProjects.onlyTable(project);
        var view = new SimpleRulesTableReader().read(table);
        var listed = new LinkedHashMap<String, Object>();
        // as the API lists them: an enumeration by the name of its constant
        view.properties.forEach((name, value) -> listed.put(name, value instanceof Object[] values
                ? Arrays.stream(values).map(item -> item instanceof Enum<?> constant ? constant.name() : item).toList()
                : value));
        new SimpleRulesWriter(table).write(SimpleRulesView.builder()
                .name(view.name)
                .returnType(view.returnType)
                .args(view.args)
                .properties(listed)
                .headers(view.headers)
                .rules(view.rules)
                .build());
    }

    private static SimpleRulesView read(Path project) {
        return new SimpleRulesTableReader().read(TableTestProjects.onlyTable(project));
    }

}
