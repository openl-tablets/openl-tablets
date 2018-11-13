package org.openl.rules.testmethod.export;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.openl.rules.testmethod.ITestUnit;
import org.openl.rules.testmethod.TestUnitsResults;

public class RulesResultExport extends ResultExport {
    @Override
    protected int writeInfo(Sheet sheet, TestUnitsResults result, int rowNum) {
        return rowNum;
    }

    @Override
    protected void writeResultHeader(TestUnitsResults result, Row row, int colNum) {
        createCell(row, colNum, "Result", styles.header);
    }

    @Override
    protected void writeResult(Row row, int colNum, ITestUnit testUnit) {
        createCell(row, colNum, testUnit.getActualParam(), styles.resultSuccess);
    }
}
