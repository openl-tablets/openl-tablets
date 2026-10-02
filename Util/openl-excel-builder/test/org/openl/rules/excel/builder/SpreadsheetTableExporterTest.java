package org.openl.rules.excel.builder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import static org.openl.rules.excel.builder.SheetCells.rowTexts;
import static org.openl.rules.excel.builder.SheetCells.text;
import static org.openl.rules.excel.builder.export.SpreadsheetResultTableExporter.SPR_RESULT_SHEET;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import org.openl.rules.model.scaffolding.ParameterModel;
import org.openl.rules.model.scaffolding.ProjectModel;
import org.openl.rules.model.scaffolding.SpreadsheetModel;
import org.openl.rules.model.scaffolding.StepModel;
import org.openl.rules.model.scaffolding.TypeInfo;

class SpreadsheetTableExporterTest {

    private static final String TEST_PROJECT = "spr_test_project";
    private static final int TOP_MARGIN = 2;

    @Test
    void testSpreadsheetExport() throws IOException {
        var resultModel = new SpreadsheetModel();
        resultModel.setType("Double");
        resultModel.setName("TestDoubleSpr");

        var inputParameter = new ParameterModel(new TypeInfo(String.class), "name");
        resultModel.setParameters(List.of(inputParameter));
        var doubleStep = new StepModel("simpleCalculation", "Double", "=0.0d");
        var stringStep = new StepModel("calculateName", "String", "=" + "\"\"");
        var sprStep = new StepModel("calculateIndex", "IndexCalculation", "=new IndexCalculation()");
        var booleanStep = new StepModel("booleanStep", "Boolean", "=false");
        var dateStep = new StepModel("dateStep", "Date", "=new Date()");
        var integerStep = new StepModel("integerStep", "Integer", "=0");
        var longStep = new StepModel("longStep", "Long", "=0L");
        resultModel
                .setSteps(Arrays.asList(doubleStep, stringStep, sprStep, booleanStep, dateStep, integerStep, longStep));

        var projectModel = new ProjectModel(TEST_PROJECT,
                false,
                Set.of(),
                List.of(),
                List.of(resultModel),
                List.of());

        ExcelFileBuilder.generateProject(projectModel);

        try (var wb = new XSSFWorkbook(new FileInputStream("../openl-excel-builder/spr_test_project.xlsx"))) {
            var dtsSheet = wb.getSheet(SPR_RESULT_SHEET);
            assertNotNull(dtsSheet);
            assertEquals("Spreadsheet Double TestDoubleSpr ( String name )", text(dtsSheet, TOP_MARGIN, 1));
            assertEquals(List.of("Step", "Formula"), rowTexts(dtsSheet, TOP_MARGIN + 1, 1, 2));
            assertEquals(List.of("simpleCalculation", "=0.0d"), rowTexts(dtsSheet, TOP_MARGIN + 2, 1, 2));
            assertEquals(List.of("calculateName", "=\"\""), rowTexts(dtsSheet, TOP_MARGIN + 3, 1, 2));
            assertEquals(List.of("calculateIndex", "=new IndexCalculation()"),
                    rowTexts(dtsSheet, TOP_MARGIN + 4, 1, 2));
            assertEquals(List.of("booleanStep", "=false"), rowTexts(dtsSheet, TOP_MARGIN + 5, 1, 2));
            assertEquals(List.of("dateStep", "=new Date()"), rowTexts(dtsSheet, TOP_MARGIN + 6, 1, 2));
            assertEquals(List.of("integerStep", "=0"), rowTexts(dtsSheet, TOP_MARGIN + 7, 1, 2));
            assertEquals(List.of("longStep", "=0L"), rowTexts(dtsSheet, TOP_MARGIN + 8, 1, 2));
        }

    }

    @AfterAll
    static void clean() throws IOException {
        var dir = new File("../openl-excel-builder");
        var files = dir.listFiles();
        assertNotNull(files);
        for (File file : files) {
            if (file.getName().equals("spr_test_project.xlsx")) {
                Files.delete(file.toPath());
                break;
            }
        }
    }
}
