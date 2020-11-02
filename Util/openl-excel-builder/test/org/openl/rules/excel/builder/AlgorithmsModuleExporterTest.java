package org.openl.rules.excel.builder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.openl.rules.excel.builder.export.DataTableExporter.DATA_SHEET;
import static org.openl.rules.excel.builder.export.EnvironmentTableExporter.ENV_SHEET;
import static org.openl.rules.excel.builder.export.SpreadsheetResultTableExporter.SPR_RESULT_SHEET;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collections;

import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.AfterClass;
import org.junit.Test;
import org.openl.rules.model.scaffolding.DatatypeModel;
import org.openl.rules.model.scaffolding.FieldModel;
import org.openl.rules.model.scaffolding.ParameterModel;
import org.openl.rules.model.scaffolding.SpreadsheetModel;
import org.openl.rules.model.scaffolding.StepModel;
import org.openl.rules.model.scaffolding.data.DataModel;
import org.openl.rules.model.scaffolding.environment.EnvironmentModel;

public class AlgorithmsModuleExporterTest {

    public static final String ALGORITHMS = "Algorithms.xlsx";
    public static final int DEFAULT_MARGIN = 2;
    public static final int DEFAULT_CELL = 1;

    @Test
    public void testAlgorithmsModuleGeneration() throws IOException {
        EnvironmentModel environmentModel = new EnvironmentModel(Arrays.asList("Apple", "Car"),
            Arrays.asList("Building", "Person"));

        SpreadsheetModel resultModel = new SpreadsheetModel();
        resultModel.setType("String");
        resultModel.setName("TestSpr");

        resultModel
            .setParameters(Arrays.asList(new ParameterModel("Integer", "id"), new ParameterModel("Integer", "count")));

        StepModel longStep = new StepModel("balance", "Long", "=0L");
        StepModel formulaStepUpperCase = new StepModel("Formula", "String", "=Test");
        StepModel formulaStepLowerCase = new StepModel("formula", "String", "=Test");
        StepModel valueStep = new StepModel("Step", "String", "=Test");
        StepModel formulaOneStep = new StepModel("Formula1", "String", "=Test");
        resultModel
            .setSteps(Arrays.asList(longStep, formulaStepLowerCase, formulaStepUpperCase, valueStep, formulaOneStep));

        DatatypeModel dt = new DatatypeModel("Test");
        FieldModel stringField = new FieldModel("type", "String", "Hello, World");
        dt.setFields(Collections.singletonList(stringField));

        DataModel testModel = new DataModel("getTest", "Test", null, dt);

        try (ByteArrayOutputStream algorithmsFileOutputSteam = new ByteArrayOutputStream()) {
            ExcelFileBuilder.generateAlgorithmsModule(Collections.singletonList(resultModel),
                Collections.singletonList(testModel),
                algorithmsFileOutputSteam,
                environmentModel);
            try (OutputStream fos = new FileOutputStream(ALGORITHMS)) {
                fos.write(algorithmsFileOutputSteam.toByteArray());
            }
        }

        try (XSSFWorkbook wb = new XSSFWorkbook(new FileInputStream("../openl-excel-builder/" + ALGORITHMS))) {
            XSSFSheet sprSheet = wb.getSheet(SPR_RESULT_SHEET);
            assertNotNull(sprSheet);

            XSSFRow sprHeader = sprSheet.getRow(DEFAULT_MARGIN);
            assertNotNull(sprHeader);
            String sprHeaderText = sprHeader.getCell(DEFAULT_CELL).getStringCellValue();
            assertEquals("Spreadsheet String TestSpr ( Integer id, Integer count )", sprHeaderText);

            XSSFRow subHeaderRow = sprSheet.getRow(DEFAULT_MARGIN + 1);
            assertNotNull(subHeaderRow);
            XSSFCell stepHeaderCell = subHeaderRow.getCell(DEFAULT_CELL);
            XSSFCell valueHeaderCell = subHeaderRow.getCell(DEFAULT_CELL + 1);
            assertNotNull(stepHeaderCell);
            assertNotNull(valueHeaderCell);
            String stepHeaderText = stepHeaderCell.getStringCellValue();
            String valueHeaderText = valueHeaderCell.getStringCellValue();
            assertEquals("Step", stepHeaderText);
            assertEquals("Formula11", valueHeaderText);

            XSSFRow longStepRow = sprSheet.getRow(DEFAULT_MARGIN + 2);
            assertNotNull(longStep);
            String longText = longStepRow.getCell(DEFAULT_CELL).getStringCellValue();
            assertEquals("balance", longText);

            XSSFRow formulaFirstStepRow = sprSheet.getRow(DEFAULT_MARGIN + 3);
            assertNotNull(formulaFirstStepRow);
            String firstFormulaText = formulaFirstStepRow.getCell(DEFAULT_CELL).getStringCellValue();
            assertEquals("formula", firstFormulaText);

            XSSFRow formulaSecondStepRow = sprSheet.getRow(DEFAULT_MARGIN + 4);
            assertNotNull(formulaSecondStepRow);
            String secondFormulaText = formulaSecondStepRow.getCell(DEFAULT_CELL).getStringCellValue();
            assertEquals("Formula", secondFormulaText);

            XSSFRow valueStepRow = sprSheet.getRow(DEFAULT_MARGIN + 5);
            assertNotNull(valueStepRow);
            String valueText = valueStepRow.getCell(DEFAULT_CELL).getStringCellValue();
            assertEquals("Step", valueText);

            XSSFRow formulaOneRow = sprSheet.getRow(DEFAULT_MARGIN + 6);
            assertNotNull(formulaOneRow);
            String formulaOneText = formulaOneRow.getCell(DEFAULT_CELL).getStringCellValue();
            assertEquals("Formula1", formulaOneText);

            XSSFSheet dtsSheet = wb.getSheet(DATA_SHEET);
            assertNotNull(dtsSheet);
            XSSFRow dtHeader = dtsSheet.getRow(DEFAULT_MARGIN);
            assertNotNull(dtHeader);
            String dtHeaderText = dtHeader.getCell(DEFAULT_CELL).getStringCellValue();
            assertEquals("Data Test getTest", dtHeaderText);

            XSSFRow subheaderRow = dtsSheet.getRow(DEFAULT_MARGIN + 1);
            assertNotNull(subheaderRow);

            XSSFCell typeSbCell = subheaderRow.getCell(DEFAULT_CELL);
            assertNotNull(typeSbCell);
            String typeSubheader = typeSbCell.getStringCellValue();
            assertEquals("type", typeSubheader);

            XSSFRow columnHeaderRow = dtsSheet.getRow(DEFAULT_MARGIN + 2);
            assertNotNull(columnHeaderRow);

            XSSFCell typeColumnHeaderCell = columnHeaderRow.getCell(DEFAULT_CELL);
            assertNotNull(typeColumnHeaderCell);
            String typeColumnHeader = typeColumnHeaderCell.getStringCellValue();
            assertEquals("Type", typeColumnHeader);

            XSSFRow valueRow = dtsSheet.getRow(DEFAULT_MARGIN + 3);
            assertNotNull(valueRow);

            XSSFCell typeValueCell = valueRow.getCell(DEFAULT_CELL);
            assertNotNull(typeValueCell);
            String typeValue = typeValueCell.getStringCellValue();
            assertEquals("Hello, World", typeValue);

            XSSFSheet envSheet = wb.getSheet(ENV_SHEET);
            assertNotNull(envSheet);

            XSSFRow envHeaderRow = envSheet.getRow(DEFAULT_MARGIN);
            assertNotNull(envHeaderRow);
            XSSFCell envHeaderRowCell = envHeaderRow.getCell(DEFAULT_CELL);
            assertNotNull(envHeaderRowCell);
            assertEquals("Environment", envHeaderRowCell.getStringCellValue());

            XSSFRow firstDependencyRow = envSheet.getRow(DEFAULT_MARGIN + 1);
            assertNotNull(firstDependencyRow);
            XSSFCell dpCell = firstDependencyRow.getCell(DEFAULT_CELL);
            assertNotNull(dpCell);
            assertEquals("dependency", dpCell.getStringCellValue());
            XSSFCell valueCell = firstDependencyRow.getCell(DEFAULT_CELL + 1);
            assertNotNull(valueCell);
            assertEquals("Building", valueCell.getStringCellValue());

            XSSFRow secondDependencyRow = envSheet.getRow(DEFAULT_MARGIN + 2);
            assertNotNull(secondDependencyRow);
            XSSFCell dpSecondCell = secondDependencyRow.getCell(DEFAULT_CELL);
            assertNotNull(dpSecondCell);
            assertEquals("dependency", dpSecondCell.getStringCellValue());
            XSSFCell valueSecondCell = secondDependencyRow.getCell(DEFAULT_CELL + 1);
            assertNotNull(valueSecondCell);
            assertEquals("Person", valueSecondCell.getStringCellValue());

            XSSFRow firstImportRow = envSheet.getRow(DEFAULT_MARGIN + 3);
            assertNotNull(firstImportRow);
            XSSFCell impCell = firstImportRow.getCell(DEFAULT_CELL);
            assertNotNull(impCell);
            assertEquals("import", impCell.getStringCellValue());
            XSSFCell impValueCell = firstImportRow.getCell(DEFAULT_CELL + 1);
            assertNotNull(impValueCell);
            assertEquals("Apple", impValueCell.getStringCellValue());

            XSSFRow secondImportRow = envSheet.getRow(DEFAULT_MARGIN + 4);
            assertNotNull(secondImportRow);
            XSSFCell impSecondCell = secondImportRow.getCell(DEFAULT_CELL);
            assertNotNull(impSecondCell);
            assertEquals("import", impSecondCell.getStringCellValue());
            XSSFCell impSecondValueCell = secondImportRow.getCell(DEFAULT_CELL + 1);
            assertNotNull(impSecondValueCell);
            assertEquals("Car", impSecondValueCell.getStringCellValue());
        }
    }

    @AfterClass
    public static void clean() throws IOException {
        File dir = new File("../openl-excel-builder");
        File[] files = dir.listFiles();
        assertNotNull(files);
        for (File file : files) {
            if (file.getName().equals(ALGORITHMS)) {
                Files.delete(file.toPath());
                break;
            }
        }
    }
}
