package org.openl.rules.webstudio.web.test.export;

import org.apache.poi.ss.usermodel.*;
import org.openl.rules.table.formatters.FormattersManager;
import org.openl.rules.testmethod.*;
import org.openl.rules.testmethod.result.ComparedResult;
import org.openl.rules.ui.TableSyntaxNodeUtils;

public class TestResultExport extends ResultExport {

    @Override
    protected int writeInfo(Sheet sheet, TestUnitsResults result, int rowNum) {
        TestSuite testSuite = result.getTestSuite();
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
        return rowNum;
    }

    @Override
    protected void writeResultHeader(TestUnitsResults result, Row row, int colNum) {
        for (String name : result.getTestResultColumnDisplayNames()) {
            createCell(row, colNum++, name, styles.header);
        }
    }

    @Override
    protected void writeResult(Row row, int colNum, ITestUnit testUnit) {
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

    private String getTestName(TestSuite testSuite) {
        return TableSyntaxNodeUtils.getTestName(testSuite.getTestSuiteMethod());
    }
}
