package org.openl.studio.projects.service.tables;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;

import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.openl.studio.projects.model.tables.SimpleSpreadsheetView;
import org.openl.studio.projects.service.tables.write.TableWriterExecutor;
import org.openl.studio.projects.service.tables.write.TableWritersFactory;

class SimpleSpreadsheetTableIOTest {

    @TempDir
    Path tempDir;

    @Test
    void writesNewTablePropertiesInRequestOrder() throws IOException {
        var project = TableTestProjects.writeProject(tempDir.resolve("properties"), "properties", "Rules",
                new String[][]{{"Rules String Hello()", null}, {"", "Hello"}});
        var properties = new LinkedHashMap<String, Object>();
        properties.put("state", "AL");
        properties.put("lob", "TEST");
        var view = SimpleSpreadsheetView.builder()
                .name("WithProperties")
                .properties(properties)
                .returnType("String")
                .build();

        var writer = new TableWritersFactory()
                .getNewTableWriter(view, TableTestProjects.sheetGrid(project, "WithProperties"));
        new TableWriterExecutor().executeWrite(writer, view);

        try (var input = Files.newInputStream(project.resolve("properties.xlsx"));
                var workbook = new XSSFWorkbook(input)) {
            var sheet = workbook.getSheet("WithProperties");
            assertEquals("state", sheet.getRow(2).getCell(2).getStringCellValue());
            assertEquals("lob", sheet.getRow(3).getCell(2).getStringCellValue());
        }
    }
}
