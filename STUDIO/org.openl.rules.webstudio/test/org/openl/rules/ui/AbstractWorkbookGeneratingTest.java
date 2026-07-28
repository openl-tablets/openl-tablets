package org.openl.rules.ui;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.junit.jupiter.api.io.TempDir;

import org.openl.rules.project.model.Module;
import org.openl.rules.project.resolving.ProjectResolver;
import org.openl.rules.project.resolving.ProjectResolvingException;

abstract class AbstractWorkbookGeneratingTest {
    @TempDir
    Path tempFolder;

    protected List<Module> getModules() throws ProjectResolvingException {
        var rulesFolder = tempFolder;
        var projectDescriptor = ProjectResolver.getInstance().resolve(rulesFolder);
        return projectDescriptor.getModules();
    }

    protected void createTable(Sheet sheet, String[][] table) {
        var firstRow = sheet.getLastRowNum() + 2;
        for (var i = 0; i < table.length; i++) {
            var row = sheet.createRow(firstRow + i);
            for (var j = 0; j < table[i].length; j++) {
                var cell = row.createCell(j);
                cell.setCellValue(table[i][j]);
            }
        }

    }

    protected void writeBook(Workbook wb, String file) throws IOException {
        try (var os = new BufferedOutputStream(Files.newOutputStream(tempFolder.resolve(file)))) {
            wb.write(os);
        }
    }
}
