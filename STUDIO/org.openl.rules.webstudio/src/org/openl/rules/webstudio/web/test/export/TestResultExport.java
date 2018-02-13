package org.openl.rules.webstudio.web.test.export;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.openl.rules.table.formatters.FormattersManager;
import org.openl.rules.testmethod.*;
import org.openl.rules.testmethod.result.ComparedResult;
import org.openl.rules.ui.TableSyntaxNodeUtils;
import org.openl.rules.webstudio.web.test.ParameterWithValueAndPreviewDeclaration;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.util.FileUtils;

public class TestResultExport implements AutoCloseable {
    static final int FIRST_COLUMN = 1;
    static final int FIRST_ROW = 2;
    static final int SPACE_BETWEEN_RESULTS = 3;

    private final TestUnitsResults[] results;
    private final int testsPerPage;

    private File tempFile;
    private Styles styles;
    private Map<IOpenClass, List<ParameterWithValueDeclaration>> allParams = new LinkedHashMap<>();

    public TestResultExport(TestUnitsResults[] results, int testsPerPage) {
        this.results = results;
        this.testsPerPage = testsPerPage;
    }

    public File createExcelFile() throws IOException {
        close(); // Clear previous file if invoked twice

        SXSSFWorkbook workbook = new SXSSFWorkbook();
        try {
            styles = new Styles(workbook);
            ParameterExport parameterExport = new ParameterExport(styles);

            SXSSFSheet sheet = workbook.createSheet("Result " + 1);
            sheet.trackAllColumnsForAutoSizing();
            int rowNum = FIRST_ROW;
            for (int i = 0; i < results.length; i++) {
                if (testsPerPage > 0) {
                    int pageNum = i / testsPerPage + 1;
                    int inPage = i % testsPerPage;
                    if (inPage == 0 && pageNum > 1) {
                        // AutoSize previous sheet
                        autoSizeColumns(sheet);

                        sheet = workbook.createSheet("Result " + pageNum);
                        sheet.trackAllColumnsForAutoSizing();
                        rowNum = FIRST_ROW;
                    }
                }

                rowNum = write(sheet, results[i], rowNum) + SPACE_BETWEEN_RESULTS;
            }
            autoSizeColumns(sheet);

            for (Map.Entry<IOpenClass, List<ParameterWithValueDeclaration>> entry : allParams.entrySet()) {
                IOpenClass type = entry.getKey();
                String typeName = type.isArray() ? type.getComponentClass().getName() : type.getName();
                sheet = workbook.createSheet("Type " + typeName);
                sheet.trackAllColumnsForAutoSizing();
                parameterExport.write(sheet, type, entry.getValue());
                autoSizeColumns(sheet);
            }

            tempFile = File.createTempFile("test-results", ".xlsx");
            workbook.write(new FileOutputStream(tempFile));
            workbook.close();
        } finally {
            workbook.dispose();
        }
        return tempFile;
    }

    @Override
    public void close() {
        styles = null;
        FileUtils.deleteQuietly(tempFile);
        allParams.clear();
    }

    private void autoSizeColumns(SXSSFSheet sheet) {
        short lastColumn = sheet.getRow(sheet.getLastRowNum()).getLastCellNum();

        // Skip column with Test name and ID column
        for (int i = FIRST_COLUMN + 1; i < lastColumn; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    private int write(Sheet sheet, TestUnitsResults result, int startRow) {
        TestSuite testSuite = result.getTestSuite();
        int rowNum = startRow;
        int failures = result.getNumberOfFailures();

        Row row = sheet.createRow(rowNum++);
        createCell(row,
                FIRST_COLUMN,
                getTestName(testSuite),
                failures > 0 ? styles.testNameFailure : styles.testNameSuccess);

        row = sheet.createRow(rowNum++);
        String testInfo = ProjectHelper.getTestInfo(testSuite);
        if (failures > 0) {
            testInfo += " (" + failures + " failed)";
        }
        createCell(row, FIRST_COLUMN, testInfo, styles.testInfo);

        rowNum++; // Skip one row

        rowNum = writeHeader(sheet, result, rowNum);
        rowNum = writeResults(sheet, result, rowNum);

        return rowNum;
    }

    private int writeHeader(Sheet sheet, TestUnitsResults result, int rowNum) {
        Row row = sheet.createRow(rowNum++);
        int colNum = FIRST_COLUMN;
        createCell(row, colNum++, "ID", styles.header);
        createCell(row, colNum++, "Status", styles.header);

        if (result.hasDescription()) {
            createCell(row, colNum++, "Description", styles.header);
        }

        // Context
        if (result.hasContext()) {
            for (String name : result.getContextColumnDisplayNames()) {
                createCell(row, colNum++, name, styles.header);
            }
        }

        // Input data
        for (String name : result.getTestDataColumnDisplayNames()) {
            createCell(row, colNum++, name, styles.header);
        }

        // Result
        for (String name : result.getTestResultColumnDisplayNames()) {
            createCell(row, colNum++, name, styles.header);
        }

        return rowNum;
    }

    private int writeResults(Sheet sheet, TestUnitsResults result, int rowNum) {
        Row row;
        int colNum;
        for (TestUnit testUnit : result.getTestUnits()) {
            TestStatus testStatus = testUnit.compareResult();
            boolean ok = testStatus == TestStatus.TR_OK;

            row = sheet.createRow(rowNum++);
            // ID
            colNum = FIRST_COLUMN;
            createCell(row, colNum++, testUnit.getTest().getId(), ok ? styles.resultSuccessId : styles.resultFailureId);

            // Status
            String status;
            switch (testStatus) {
                case TR_OK:
                    status = "Passed";
                    break;
                case TR_NEQ:
                    status = "Failed";
                    break;
                case TR_EXCEPTION:
                    status = "Error";
                    break;
                default:
                    throw new UnsupportedOperationException();

            }
            createCell(row, colNum++, status, ok ? styles.resultSuccessStatus : styles.resultFailureStatus);

            // Description
            if (result.hasDescription()) {
                createCell(row, colNum++, testUnit.getDescription(), styles.resultOther);
            }

            // Context
            if (result.hasContext()) {
                for (ParameterWithValueDeclaration parameter : testUnit.getContextParams(result)) {
                    createCell(row, colNum++, parameter.getValue(), styles.resultOther);
                }
            }

            // Input data
            for (ParameterWithValueDeclaration parameter : testUnit.getTest().getExecutionParams()) {
                IOpenClass type = parameter.getType();
                if (!type.isSimple()) {
                    List<ParameterWithValueDeclaration> paramsForType = allParams.get(type);
                    if (paramsForType == null) {
                        paramsForType = new ArrayList<>();
                        allParams.put(type, paramsForType);
                    }
                    paramsForType.add(parameter);
                }

                createCell(row, colNum++, parameter, styles.resultOther);
            }

            // Result
            for (ComparedResult parameter : testUnit.getResultParams()) {
                boolean okField = parameter.getStatus() == TestStatus.TR_OK;

                Cell cell = createCell(row,
                        colNum++,
                        parameter.getActualValue(),
                        okField ? styles.resultSuccess : styles.resultFailure);

                if (!okField) {
                    String expected = "Expected: ";
                    Object expectedValue = getSimpleValue(parameter.getExpectedValue());
                    if (expectedValue != null) {
                        expected += FormattersManager.format(expectedValue);
                    }
                    setCellComment(cell, expected);
                }
            }
        }
        return rowNum;
    }

    private Cell createCell(Row row, int cellNum, Object value, CellStyle style) {
        Cell cell = row.createCell(cellNum);

        Object simpleValue = getSimpleValue(value);
        if (simpleValue != null) {
            if (simpleValue instanceof Date) {
                style = styles.getDateStyle(row.getSheet().getWorkbook(), style);
                cell.setCellValue((Date) simpleValue);
            } else {
                cell.setCellValue(FormattersManager.format(simpleValue));
            }
        }

        cell.setCellStyle(style);

        return cell;
    }

    private Object getSimpleValue(Object value) {
        if (value == null) {
            return null;
        }

        if (value instanceof ParameterWithValueDeclaration) {
            Object simpleValue = ((ParameterWithValueDeclaration) value).getValue();

            if (value instanceof ParameterWithValueAndPreviewDeclaration) {
                // Return preview field for complex objects
                IOpenField previewField = ((ParameterWithValueAndPreviewDeclaration) value).getPreviewField();
                if (previewField != null) {
                    // If preview can't be found, return the object itself
                    Object previewValue = ExportUtils.fieldValue(simpleValue, previewField);
                    simpleValue = previewValue == null ? simpleValue : previewValue;
                }
            }

            return getSimpleValue(simpleValue);
        }

        return value;
    }

    private String getTestName(TestSuite testSuite) {
        return TableSyntaxNodeUtils.getTestName(testSuite.getTestSuiteMethod());
    }

    private static void setCellComment(Cell cell, String message) {
        CreationHelper factory = cell.getSheet().getWorkbook().getCreationHelper();
        ClientAnchor anchor = factory.createClientAnchor();
        anchor.setCol1(cell.getColumnIndex());
        anchor.setCol2(cell.getColumnIndex() + 3);
        anchor.setRow1(cell.getRowIndex());
        anchor.setRow2(cell.getRowIndex() + 3);

        Comment comment = cell.getSheet().createDrawingPatriarch().createCellComment(anchor);
        comment.setString(factory.createRichTextString(message));
        comment.setAuthor("OpenL");

        // Assign the comment to the cell
        cell.setCellComment(comment);
    }
}
