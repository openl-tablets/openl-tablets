package org.openl.rules.ui;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;
import org.openl.rules.project.model.Module;
import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.rules.project.resolving.ProjectResolver;
import org.openl.rules.project.resolving.ProjectResolvingException;

public abstract class AbstractWorkbookGeneratingTest {
    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    protected List<Module> getModules() throws ProjectResolvingException {
        File rulesFolder = tempFolder.getRoot();
        ProjectDescriptor projectDescriptor = ProjectResolver.getInstance().resolve(rulesFolder);
        return projectDescriptor.getModules();
    }

    protected void createTable(Sheet sheet, String[][] table) {
        int firstRow = sheet.getLastRowNum() + 2;
        for (int i = 0; i < table.length; i++) {
            Row row = sheet.createRow(firstRow + i);
            for (int j = 0; j < table[i].length; j++) {
                Cell cell = row.createCell(j);
                cell.setCellValue(table[i][j]);
            }
        }

    }

    protected void writeBook(Workbook wb, String file) throws IOException {
        try (OutputStream os = new BufferedOutputStream(new FileOutputStream(new File(tempFolder.getRoot(), file)))) {
            wb.write(os);
        }
    }
}
