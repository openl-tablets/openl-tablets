package org.openl.rules.testmethod.export;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;

import org.openl.rules.lang.xls.TableSyntaxNodeUtils;
import org.openl.rules.table.formatters.FormattersManager;
import org.openl.rules.testmethod.ITestUnit;
import org.openl.rules.testmethod.ProjectHelper;
import org.openl.rules.testmethod.TestStatus;
import org.openl.rules.testmethod.TestUnitsResults;
import org.openl.rules.testmethod.result.ComparedResult;

public class TestResultExport extends ResultExport {

    @Override
    protected int writeInfo(Sheet sheet, TestUnitsResults result, int rowNum) {
        var testSuite = result.getTestSuite();
        var failures = result.getNumberOfFailures();

        var row = sheet.createRow(rowNum++);
        String testName = TableSyntaxNodeUtils.getTestName(testSuite.getTestSuiteMethod());
        createCell(row, FIRST_COLUMN, testName, failures > 0 ? styles.testNameFailure : styles.testNameSuccess);

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
            var okField = parameter.getStatus() == TestStatus.TR_OK;

            var cell = createCell(row,
                    colNum++,
                    parameter.getActualValue(),
                    okField ? styles.resultSuccess : styles.resultFailure);

            if (!okField) {
                var expected = new StringBuilder("Expected: ");
                var expectedValue = getSimpleValue(parameter.getExpectedValue());
                if (expectedValue != null) {
                    expected.append(FormattersManager.format(expectedValue));
                }
                setCellComment(cell, expected.toString());
            }
        }
    }

}
