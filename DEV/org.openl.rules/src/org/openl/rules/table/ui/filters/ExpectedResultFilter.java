package org.openl.rules.table.ui.filters;

import java.util.HashMap;
import java.util.Map;

import org.openl.rules.table.FormattedCell;
import org.openl.rules.table.Point;
import org.openl.rules.table.formatters.FormattersManager;
import org.openl.rules.testmethod.IParameterWithValueDeclaration;
import org.openl.rules.testmethod.TestUnitResultComparator.TestStatus;
import org.openl.rules.testmethod.result.ComparedResult;

public class ExpectedResultFilter extends AGridFilter {

    private Map<Point, ComparedResult> spreadsheetCellsForTest;

    public ExpectedResultFilter(Map<Point, ComparedResult> spreadsheetCellsForTest) {
        this.spreadsheetCellsForTest = new HashMap<Point, ComparedResult>(spreadsheetCellsForTest);
    }

    public FormattedCell filterFormat(FormattedCell cell) {
        Point cellCoordinates = new Point(cell.getAbsoluteColumn(), cell.getAbsoluteRow());

        if (spreadsheetCellsForTest.containsKey(cellCoordinates)) {
            ComparedResult result = spreadsheetCellsForTest.get(cellCoordinates);
            StringBuilder formattedValue = new StringBuilder(60);
            boolean isOk = (result.getStatus() == TestStatus.TR_OK);
            if (isOk) {
                formattedValue.append("<i class=\"case-success\"></i> ").append(cell.getFormattedValue());
            } else {
                Object expectedValue = result.getExpectedValue();
                if (expectedValue instanceof IParameterWithValueDeclaration) {
                    IParameterWithValueDeclaration declaration = (IParameterWithValueDeclaration) expectedValue;
                    expectedValue = declaration.getValue();
                }
                String formattedExpectedValue = FormattersManager.format(expectedValue);
                if (formattedExpectedValue == null || formattedExpectedValue.isEmpty() || expectedValue == null) {
                    formattedExpectedValue = "<span class=\"case-empty\">Empty</span>";
                }
                formattedValue.append("<i class=\"case-error\"></i> ")
                    .append(cell.getFormattedValue())
                    .append(' ')
                    .append("<span class=\"case-expected\">Expected: </span>")
                    .append(formattedExpectedValue);
            }
            cell.setFormattedValue(formattedValue.toString());
        }

        return cell;
    }
}
