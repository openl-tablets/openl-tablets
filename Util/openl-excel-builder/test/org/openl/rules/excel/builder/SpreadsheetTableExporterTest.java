package org.openl.rules.excel.builder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

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
            var headerRow = dtsSheet.getRow(TOP_MARGIN);
            assertNotNull(headerRow);
            var headerText = headerRow.getCell(1).getStringCellValue();
            assertEquals("Spreadsheet Double TestDoubleSpr ( String name )", headerText);
            var sprSubHeaderRow = dtsSheet.getRow(TOP_MARGIN + 1);
            assertNotNull(sprSubHeaderRow);
            var stepHeaderCell = sprSubHeaderRow.getCell(1);
            assertNotNull(stepHeaderCell);
            assertEquals("Step", stepHeaderCell.getStringCellValue());
            var valueHeaderCell = sprSubHeaderRow.getCell(2);
            assertNotNull(valueHeaderCell);
            assertEquals("Formula", valueHeaderCell.getStringCellValue());

            var firstStepRow = dtsSheet.getRow(TOP_MARGIN + 2);
            assertNotNull(firstStepRow);
            var nameCell = firstStepRow.getCell(1);
            assertNotNull(nameCell);
            assertEquals("simpleCalculation", nameCell.getStringCellValue());
            var valueCell = firstStepRow.getCell(2);
            assertNotNull(valueCell);
            assertEquals("=0.0d", valueCell.getStringCellValue());

            var secondStepRow = dtsSheet.getRow(TOP_MARGIN + 3);
            assertNotNull(secondStepRow);
            var secondNameCell = secondStepRow.getCell(1);
            assertNotNull(secondNameCell);
            assertEquals("calculateName", secondNameCell.getStringCellValue());
            var secondValueCell = secondStepRow.getCell(2);
            assertNotNull(secondValueCell);
            assertEquals("=\"\"", secondValueCell.getStringCellValue());

            var sprCallStepRow = dtsSheet.getRow(TOP_MARGIN + 4);
            assertNotNull(sprCallStepRow);
            var sprNameCell = sprCallStepRow.getCell(1);
            assertNotNull(sprNameCell);
            assertEquals("calculateIndex", sprNameCell.getStringCellValue());
            var sprCellCall = sprCallStepRow.getCell(2);
            assertNotNull(sprCellCall);
            assertEquals("=new IndexCalculation()", sprCellCall.getStringCellValue());

            var booleanRow = dtsSheet.getRow(TOP_MARGIN + 5);
            assertNotNull(booleanRow);
            var boolCell = booleanRow.getCell(1);
            assertNotNull(boolCell);
            assertEquals("booleanStep", boolCell.getStringCellValue());
            var boolValueCell = booleanRow.getCell(2);
            assertNotNull(boolValueCell);
            assertEquals("=false", boolValueCell.getStringCellValue());

            var dateRow = dtsSheet.getRow(TOP_MARGIN + 6);
            assertNotNull(dateRow);
            var dateNameCell = dateRow.getCell(1);
            assertNotNull(dateNameCell);
            assertEquals("dateStep", dateNameCell.getStringCellValue());
            var dateValueCell = dateRow.getCell(2);
            assertEquals("=new Date()", dateValueCell.getStringCellValue());

            var integerStepRow = dtsSheet.getRow(TOP_MARGIN + 7);
            assertNotNull(integerStepRow);
            var integerNameCell = integerStepRow.getCell(1);
            assertNotNull(integerNameCell);
            assertEquals("integerStep", integerNameCell.getStringCellValue());
            var integerValueCell = integerStepRow.getCell(2);
            assertEquals("=0", integerValueCell.getStringCellValue());

            var longStepRow = dtsSheet.getRow(TOP_MARGIN + 8);
            assertNotNull(longStepRow);
            var longNameCell = longStepRow.getCell(1);
            assertNotNull(longNameCell);
            assertEquals("longStep", longNameCell.getStringCellValue());
            var longValueCell = longStepRow.getCell(2);
            assertEquals("=0L", longValueCell.getStringCellValue());
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
