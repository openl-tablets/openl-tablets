package org.openl.rules.excel.builder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import static org.openl.rules.excel.builder.export.EnvironmentTableExporter.ENV_SHEET;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Set;

import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import org.openl.rules.model.scaffolding.ProjectModel;
import org.openl.rules.model.scaffolding.environment.EnvironmentModel;
import org.openl.util.CollectionUtils;

class EnvironmentTableExporterTest {

    private static final String TEST_PROJECT = "env_test_project";
    private static final int TOP_MARGIN = 2;
    private static final String MODEL = "Model";
    private static final String IMPORTED_VALUE = "org.openl.import.test.Test";

    @Test
    void testSpreadsheetExport() throws IOException {
        var environmentModel = new EnvironmentModel();
        environmentModel.setDependencies(List.of(MODEL));
        environmentModel.setImports(List.of(IMPORTED_VALUE));

        var projectModel = new ProjectModel(TEST_PROJECT,
                false,
                Set.of(),
                List.of(),
                List.of(),
                List.of());

        byte[] bytes;
        try (var sos = new ByteArrayOutputStream()) {
            ExcelFileBuilder.generateAlgorithmsModule(projectModel.getSpreadsheetResultModels(),
                    List.of(),
                    sos,
                    environmentModel);
            bytes = sos.toByteArray();
        }
        assertFalse(CollectionUtils.isEmpty(bytes));
        try (var spr = new ByteArrayInputStream(bytes); var wb = new XSSFWorkbook(spr)) {
            var dtsSheet = wb.getSheet(ENV_SHEET);
            assertNotNull(dtsSheet);
            var headerRow = dtsSheet.getRow(TOP_MARGIN);
            assertNotNull(headerRow);
            var headerText = headerRow.getCell(1).getStringCellValue();
            assertEquals(ENV_SHEET, headerText);

            var dependencyRow = dtsSheet.getRow(TOP_MARGIN + 1);
            assertNotNull(dependencyRow);
            var dName = dependencyRow.getCell(1);
            assertNotNull(dName);
            assertEquals("dependency", dName.getStringCellValue());
            var dValue = dependencyRow.getCell(2);
            assertNotNull(dValue);
            assertEquals(MODEL, dValue.getStringCellValue());

            var importRow = dtsSheet.getRow(TOP_MARGIN + 2);
            assertNotNull(importRow);
            var iName = importRow.getCell(1);
            assertNotNull(iName);
            assertEquals("import", iName.getStringCellValue());
            var iValue = importRow.getCell(2);
            assertNotNull(iValue);
            assertEquals(IMPORTED_VALUE, iValue.getStringCellValue());
        }
    }
}
