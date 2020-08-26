package org.openl.rules.excel.builder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;

import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.Test;
import org.openl.rules.model.scaffolding.ProjectModel;
import org.openl.rules.model.scaffolding.environment.EnvironmentModel;

public class EnvironmentTableExporterTest {

    public static final String TEST_PROJECT = "env_test_project";
    public static final int TOP_MARGIN = 2;
    public static final String MODEL = "Model";
    public static final String ENVIRONMENT = "Environment";
    public static final String IMPORTED_VALUE = "org.openl.import.test.Test";

    @Test
    public void testSpreadsheetExport() throws IOException {
        EnvironmentModel environmentModel = new EnvironmentModel();
        environmentModel.setDependencies(Collections.singletonList(MODEL));
        environmentModel.setImports(Collections.singletonList(IMPORTED_VALUE));

        ProjectModel projectModel = new ProjectModel(TEST_PROJECT, Collections.emptyList(), Collections.emptyList());

        ByteArrayOutputStream sos = new ByteArrayOutputStream();
        ExcelFileBuilder
            .generateSpreadsheetsWithEnvironment(projectModel.getSpreadsheetResultModels(), sos, environmentModel);
        byte[] bytes = sos.toByteArray();
        try (InputStream spr = new ByteArrayInputStream(bytes); XSSFWorkbook wb = new XSSFWorkbook(spr)) {
            XSSFSheet dtsSheet = wb.getSheet(ENVIRONMENT);
            assertNotNull(dtsSheet);
            XSSFRow headerRow = dtsSheet.getRow(TOP_MARGIN);
            assertNotNull(headerRow);
            String headerText = headerRow.getCell(1).getStringCellValue();
            assertEquals(ENVIRONMENT, headerText);

            XSSFRow dependencyRow = dtsSheet.getRow(TOP_MARGIN + 1);
            assertNotNull(dependencyRow);
            XSSFCell dName = dependencyRow.getCell(1);
            assertNotNull(dName);
            assertEquals("dependency", dName.getStringCellValue());
            XSSFCell dValue = dependencyRow.getCell(2);
            assertNotNull(dValue);
            assertEquals(MODEL, dValue.getStringCellValue());

            XSSFRow importRow = dtsSheet.getRow(TOP_MARGIN + 2);
            assertNotNull(importRow);
            XSSFCell iName = importRow.getCell(1);
            assertNotNull(iName);
            assertEquals("import", iName.getStringCellValue());
            XSSFCell iValue = importRow.getCell(2);
            assertNotNull(iValue);
            assertEquals(IMPORTED_VALUE, iValue.getStringCellValue());
        }
    }
}
