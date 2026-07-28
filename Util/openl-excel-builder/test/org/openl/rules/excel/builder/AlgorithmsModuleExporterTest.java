package org.openl.rules.excel.builder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import static org.openl.rules.excel.builder.export.DataTableExporter.DATA_SHEET;
import static org.openl.rules.excel.builder.export.EnvironmentTableExporter.ENV_SHEET;
import static org.openl.rules.excel.builder.export.SpreadsheetResultTableExporter.SPR_RESULT_SHEET;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collections;

import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import org.openl.rules.model.scaffolding.DatatypeModel;
import org.openl.rules.model.scaffolding.FieldModel;
import org.openl.rules.model.scaffolding.ParameterModel;
import org.openl.rules.model.scaffolding.SpreadsheetModel;
import org.openl.rules.model.scaffolding.StepModel;
import org.openl.rules.model.scaffolding.TypeInfo;
import org.openl.rules.model.scaffolding.data.DataModel;
import org.openl.rules.model.scaffolding.environment.EnvironmentModel;

class AlgorithmsModuleExporterTest {

    private static final String ALGORITHMS = "Algorithms.xlsx";
    private static final int DEFAULT_MARGIN = 2;
    private static final int DEFAULT_CELL = 1;

    @Test
    void testAlgorithmsModuleGeneration() throws IOException {
        var environmentModel = new EnvironmentModel(Arrays.asList("Apple", "Car"),
                Arrays.asList("Building", "Person"));

        var resultModel = new SpreadsheetModel();
        resultModel.setType("String");
        resultModel.setName("TestSpr");

        resultModel.setParameters(
                Arrays.asList(new ParameterModel(new TypeInfo(Integer.class), "id"),
                        new ParameterModel(new TypeInfo(Integer.class), "count")));

        var longStep = new StepModel("balance", "Long", "=0L");
        var formulaStepUpperCase = new StepModel("Formula", "String", "=Test");
        var formulaStepLowerCase = new StepModel("formula", "String", "=Test");
        var valueStep = new StepModel("Step", "String", "=Test");
        var formulaOneStep = new StepModel("Formula1", "String", "=Test");
        resultModel
                .setSteps(Arrays.asList(longStep, formulaStepLowerCase, formulaStepUpperCase, valueStep, formulaOneStep));

        var dt = new DatatypeModel("Test");
        var stringField = new FieldModel("type", "String", "Hello, World");
        dt.setFields(Collections.singletonList(stringField));

        var testModel = new DataModel("getTest", "Test", null, dt);

        try (var algorithmsFileOutputSteam = new ByteArrayOutputStream()) {
            ExcelFileBuilder.generateAlgorithmsModule(Collections.singletonList(resultModel),
                    Collections.singletonList(testModel),
                    algorithmsFileOutputSteam,
                    environmentModel);
            try (var fos = new FileOutputStream(ALGORITHMS)) {
                fos.write(algorithmsFileOutputSteam.toByteArray());
            }
        }

        try (var wb = new XSSFWorkbook(new FileInputStream("../openl-excel-builder/" + ALGORITHMS))) {
            var sprSheet = wb.getSheet(SPR_RESULT_SHEET);
            assertNotNull(sprSheet);

            var sprHeader = sprSheet.getRow(DEFAULT_MARGIN);
            assertNotNull(sprHeader);
            var sprHeaderText = sprHeader.getCell(DEFAULT_CELL).getStringCellValue();
            assertEquals("Spreadsheet String TestSpr ( Integer id, Integer count )", sprHeaderText);

            var subHeaderRow = sprSheet.getRow(DEFAULT_MARGIN + 1);
            assertNotNull(subHeaderRow);
            var stepHeaderCell = subHeaderRow.getCell(DEFAULT_CELL);
            var valueHeaderCell = subHeaderRow.getCell(DEFAULT_CELL + 1);
            assertNotNull(stepHeaderCell);
            assertNotNull(valueHeaderCell);
            var stepHeaderText = stepHeaderCell.getStringCellValue();
            var valueHeaderText = valueHeaderCell.getStringCellValue();
            assertEquals("Step", stepHeaderText);
            assertEquals("Formula11", valueHeaderText);

            var longStepRow = sprSheet.getRow(DEFAULT_MARGIN + 2);
            assertNotNull(longStep);
            var longText = longStepRow.getCell(DEFAULT_CELL).getStringCellValue();
            assertEquals("balance", longText);

            var formulaFirstStepRow = sprSheet.getRow(DEFAULT_MARGIN + 3);
            assertNotNull(formulaFirstStepRow);
            var firstFormulaText = formulaFirstStepRow.getCell(DEFAULT_CELL).getStringCellValue();
            assertEquals("formula", firstFormulaText);

            var formulaSecondStepRow = sprSheet.getRow(DEFAULT_MARGIN + 4);
            assertNotNull(formulaSecondStepRow);
            var secondFormulaText = formulaSecondStepRow.getCell(DEFAULT_CELL).getStringCellValue();
            assertEquals("Formula", secondFormulaText);

            var valueStepRow = sprSheet.getRow(DEFAULT_MARGIN + 5);
            assertNotNull(valueStepRow);
            var valueText = valueStepRow.getCell(DEFAULT_CELL).getStringCellValue();
            assertEquals("Step", valueText);

            var formulaOneRow = sprSheet.getRow(DEFAULT_MARGIN + 6);
            assertNotNull(formulaOneRow);
            var formulaOneText = formulaOneRow.getCell(DEFAULT_CELL).getStringCellValue();
            assertEquals("Formula1", formulaOneText);

            var dtsSheet = wb.getSheet(DATA_SHEET);
            assertNotNull(dtsSheet);
            var dtHeader = dtsSheet.getRow(DEFAULT_MARGIN);
            assertNotNull(dtHeader);
            var dtHeaderText = dtHeader.getCell(DEFAULT_CELL).getStringCellValue();
            assertEquals("Data Test getTest", dtHeaderText);

            var subheaderRow = dtsSheet.getRow(DEFAULT_MARGIN + 1);
            assertNotNull(subheaderRow);

            var typeSbCell = subheaderRow.getCell(DEFAULT_CELL);
            assertNotNull(typeSbCell);
            var typeSubheader = typeSbCell.getStringCellValue();
            assertEquals("type", typeSubheader);

            var columnHeaderRow = dtsSheet.getRow(DEFAULT_MARGIN + 2);
            assertNotNull(columnHeaderRow);

            var typeColumnHeaderCell = columnHeaderRow.getCell(DEFAULT_CELL);
            assertNotNull(typeColumnHeaderCell);
            var typeColumnHeader = typeColumnHeaderCell.getStringCellValue();
            assertEquals("Type", typeColumnHeader);

            var valueRow = dtsSheet.getRow(DEFAULT_MARGIN + 3);
            assertNotNull(valueRow);

            var typeValueCell = valueRow.getCell(DEFAULT_CELL);
            assertNotNull(typeValueCell);
            var typeValue = typeValueCell.getStringCellValue();
            assertEquals("Hello, World", typeValue);

            var envSheet = wb.getSheet(ENV_SHEET);
            assertNotNull(envSheet);

            var envHeaderRow = envSheet.getRow(DEFAULT_MARGIN);
            assertNotNull(envHeaderRow);
            var envHeaderRowCell = envHeaderRow.getCell(DEFAULT_CELL);
            assertNotNull(envHeaderRowCell);
            assertEquals("Environment", envHeaderRowCell.getStringCellValue());

            var firstDependencyRow = envSheet.getRow(DEFAULT_MARGIN + 1);
            assertNotNull(firstDependencyRow);
            var dpCell = firstDependencyRow.getCell(DEFAULT_CELL);
            assertNotNull(dpCell);
            assertEquals("dependency", dpCell.getStringCellValue());
            var valueCell = firstDependencyRow.getCell(DEFAULT_CELL + 1);
            assertNotNull(valueCell);
            assertEquals("Building", valueCell.getStringCellValue());

            var secondDependencyRow = envSheet.getRow(DEFAULT_MARGIN + 2);
            assertNotNull(secondDependencyRow);
            var dpSecondCell = secondDependencyRow.getCell(DEFAULT_CELL);
            assertNotNull(dpSecondCell);
            assertEquals("dependency", dpSecondCell.getStringCellValue());
            var valueSecondCell = secondDependencyRow.getCell(DEFAULT_CELL + 1);
            assertNotNull(valueSecondCell);
            assertEquals("Person", valueSecondCell.getStringCellValue());

            var firstImportRow = envSheet.getRow(DEFAULT_MARGIN + 3);
            assertNotNull(firstImportRow);
            var impCell = firstImportRow.getCell(DEFAULT_CELL);
            assertNotNull(impCell);
            assertEquals("import", impCell.getStringCellValue());
            var impValueCell = firstImportRow.getCell(DEFAULT_CELL + 1);
            assertNotNull(impValueCell);
            assertEquals("Apple", impValueCell.getStringCellValue());

            var secondImportRow = envSheet.getRow(DEFAULT_MARGIN + 4);
            assertNotNull(secondImportRow);
            var impSecondCell = secondImportRow.getCell(DEFAULT_CELL);
            assertNotNull(impSecondCell);
            assertEquals("import", impSecondCell.getStringCellValue());
            var impSecondValueCell = secondImportRow.getCell(DEFAULT_CELL + 1);
            assertNotNull(impSecondValueCell);
            assertEquals("Car", impSecondValueCell.getStringCellValue());
        }
    }

    @AfterAll
    static void clean() throws IOException {
        var dir = new File("../openl-excel-builder");
        var files = dir.listFiles();
        assertNotNull(files);
        for (File file : files) {
            if (file.getName().equals(ALGORITHMS)) {
                Files.delete(file.toPath());
                break;
            }
        }
    }
}
