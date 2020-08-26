package org.openl.rules.excel.builder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collections;

import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.AfterClass;
import org.junit.Test;
import org.openl.rules.model.scaffolding.InputParameter;
import org.openl.rules.model.scaffolding.ParameterModel;
import org.openl.rules.model.scaffolding.ProjectModel;
import org.openl.rules.model.scaffolding.SpreadsheetResultModel;
import org.openl.rules.model.scaffolding.StepModel;

public class SpreadsheetTableExporterTest {

    public static final String TEST_PROJECT = "spr_test_project";
    public static final int TOP_MARGIN = 2;

    @Test
    public void testSpreadsheetExport() throws IOException {
        SpreadsheetResultModel resultModel = new SpreadsheetResultModel();
        resultModel.setType("Double");
        resultModel.setName("TestDoubleSpr");

        InputParameter inputParameter = new ParameterModel("String", "name");
        resultModel.setParameters(Collections.singletonList(inputParameter));
        StepModel doubleStep = new StepModel("simpleCalculation", "Double");
        StepModel stringStep = new StepModel("calculateName", "String");
        StepModel sprStep = new StepModel("calculateIndex", "IndexCalculation");
        StepModel booleanStep = new StepModel("booleanStep", "Boolean");
        StepModel dateStep = new StepModel("dateStep", "Date");
        StepModel dateTimeStep = new StepModel("dateTimeStep", "OffsetDateTime");
        resultModel.setSteps(Arrays.asList(doubleStep, stringStep, sprStep, booleanStep, dateStep, dateTimeStep));

        ProjectModel projectModel = new ProjectModel(TEST_PROJECT,
            Collections.emptyList(),
            Collections.singletonList(resultModel));

        ExcelFileBuilder.generateProject(projectModel);

        try (XSSFWorkbook wb = new XSSFWorkbook(new FileInputStream("../openl-excel-builder/spr_test_project.xlsx"))) {
            XSSFSheet dtsSheet = wb.getSheet("SpreadsheetResults");
            assertNotNull(dtsSheet);
            XSSFRow headerRow = dtsSheet.getRow(TOP_MARGIN);
            assertNotNull(headerRow);
            String headerText = headerRow.getCell(1).getStringCellValue();
            assertEquals("Spreadsheet Double TestDoubleSpr ( String name )", headerText);
            XSSFRow sprSubHeaderRow = dtsSheet.getRow(TOP_MARGIN + 1);
            assertNotNull(sprSubHeaderRow);
            XSSFCell stepHeaderCell = sprSubHeaderRow.getCell(1);
            assertNotNull(stepHeaderCell);
            assertEquals("Step", stepHeaderCell.getStringCellValue());
            XSSFCell valueHeaderCell = sprSubHeaderRow.getCell(2);
            assertNotNull(valueHeaderCell);
            assertEquals("Formula", valueHeaderCell.getStringCellValue());

            XSSFRow firstStepRow = dtsSheet.getRow(TOP_MARGIN + 2);
            assertNotNull(firstStepRow);
            XSSFCell nameCell = firstStepRow.getCell(1);
            assertNotNull(nameCell);
            assertEquals("simpleCalculation", nameCell.getStringCellValue());
            XSSFCell valueCell = firstStepRow.getCell(2);
            assertNotNull(valueCell);
            assertEquals("=0.0d", valueCell.getStringCellValue());

            XSSFRow secondStepRow = dtsSheet.getRow(TOP_MARGIN + 3);
            assertNotNull(secondStepRow);
            XSSFCell secondNameCell = secondStepRow.getCell(1);
            assertNotNull(secondNameCell);
            assertEquals("calculateName", secondNameCell.getStringCellValue());
            XSSFCell secondValueCell = secondStepRow.getCell(2);
            assertNotNull(secondValueCell);
            assertEquals("=_DEFAULT_", secondValueCell.getStringCellValue());

            XSSFRow sprCallStepRow = dtsSheet.getRow(TOP_MARGIN + 4);
            assertNotNull(sprCallStepRow);
            XSSFCell sprNameCell = sprCallStepRow.getCell(1);
            assertNotNull(sprNameCell);
            assertEquals("calculateIndex", sprNameCell.getStringCellValue());
            XSSFCell sprCellCall = sprCallStepRow.getCell(2);
            assertNotNull(sprCellCall);
            assertEquals("=new IndexCalculation()", sprCellCall.getStringCellValue());

            XSSFRow booleanRow = dtsSheet.getRow(TOP_MARGIN + 5);
            assertNotNull(booleanRow);
            XSSFCell boolCell = booleanRow.getCell(1);
            assertNotNull(boolCell);
            assertEquals("booleanStep", boolCell.getStringCellValue());
            XSSFCell boolValueCell = booleanRow.getCell(2);
            assertNotNull(boolValueCell);
            assertEquals("=false", boolValueCell.getStringCellValue());

            XSSFRow dateRow = dtsSheet.getRow(TOP_MARGIN + 6);
            assertNotNull(dateRow);
            XSSFCell dateNameCell = dateRow.getCell(1);
            assertNotNull(dateNameCell);
            assertEquals("dateStep", dateNameCell.getStringCellValue());
            XSSFCell dateValueCell = dateRow.getCell(2);
            assertEquals("=new Date()", dateValueCell.getStringCellValue());

            XSSFRow dateTimeRow = dtsSheet.getRow(TOP_MARGIN + 7);
            assertNotNull(dateTimeRow);
            XSSFCell dateTimeNameCell = dateTimeRow.getCell(1);
            assertNotNull(dateTimeNameCell);
            assertEquals("dateTimeStep", dateTimeNameCell.getStringCellValue());
            XSSFCell dateTimeValueCell = dateTimeRow.getCell(2);
            assertEquals("=java.time.OffsetDateTime.now()", dateTimeValueCell.getStringCellValue());
        }

    }

    @AfterClass
    public static void clean() throws IOException {
        File dir = new File("../openl-excel-builder");
        File[] files = dir.listFiles();
        assertNotNull(files);
        for (File file : files) {
            if (file.getName().equals("spr_test_project.xlsx")) {
                Files.delete(file.toPath());
                break;
            }
        }
    }
}
