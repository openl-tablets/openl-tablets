package org.openl.rules.excel.builder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import static org.openl.rules.excel.builder.SheetCells.columnTexts;
import static org.openl.rules.excel.builder.SheetCells.rowTexts;
import static org.openl.rules.excel.builder.SheetCells.text;
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
import java.util.List;

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
        dt.setFields(List.of(stringField));

        var testModel = new DataModel("getTest", "Test", null, dt);

        try (var algorithmsFileOutputSteam = new ByteArrayOutputStream()) {
            ExcelFileBuilder.generateAlgorithmsModule(List.of(resultModel),
                    List.of(testModel),
                    algorithmsFileOutputSteam,
                    environmentModel);
            try (var fos = new FileOutputStream(ALGORITHMS)) {
                fos.write(algorithmsFileOutputSteam.toByteArray());
            }
        }

        try (var wb = new XSSFWorkbook(new FileInputStream("../openl-excel-builder/" + ALGORITHMS))) {
            var sprSheet = wb.getSheet(SPR_RESULT_SHEET);
            assertNotNull(sprSheet);
            assertEquals("Spreadsheet String TestSpr ( Integer id, Integer count )",
                    text(sprSheet, DEFAULT_MARGIN, DEFAULT_CELL));
            assertEquals(List.of("Step", "Formula11"),
                    rowTexts(sprSheet, DEFAULT_MARGIN + 1, DEFAULT_CELL, DEFAULT_CELL + 1));
            assertEquals(List.of("balance", "formula", "Formula", "Step", "Formula1"),
                    columnTexts(sprSheet, DEFAULT_MARGIN + 2, DEFAULT_MARGIN + 6, DEFAULT_CELL));

            var dtsSheet = wb.getSheet(DATA_SHEET);
            assertNotNull(dtsSheet);
            assertEquals(List.of("Data Test getTest", "type", "Type", "Hello, World"),
                    columnTexts(dtsSheet, DEFAULT_MARGIN, DEFAULT_MARGIN + 3, DEFAULT_CELL));

            var envSheet = wb.getSheet(ENV_SHEET);
            assertNotNull(envSheet);
            assertEquals("Environment", text(envSheet, DEFAULT_MARGIN, DEFAULT_CELL));
            assertEquals(List.of("dependency", "Building"),
                    rowTexts(envSheet, DEFAULT_MARGIN + 1, DEFAULT_CELL, DEFAULT_CELL + 1));
            assertEquals(List.of("dependency", "Person"),
                    rowTexts(envSheet, DEFAULT_MARGIN + 2, DEFAULT_CELL, DEFAULT_CELL + 1));
            assertEquals(List.of("import", "Apple"),
                    rowTexts(envSheet, DEFAULT_MARGIN + 3, DEFAULT_CELL, DEFAULT_CELL + 1));
            assertEquals(List.of("import", "Car"),
                    rowTexts(envSheet, DEFAULT_MARGIN + 4, DEFAULT_CELL, DEFAULT_CELL + 1));
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
