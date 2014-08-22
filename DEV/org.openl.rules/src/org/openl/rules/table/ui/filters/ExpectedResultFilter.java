package org.openl.rules.table.ui.filters;

import org.openl.rules.table.FormattedCell;
import org.openl.rules.table.Point;
import org.openl.rules.table.formatters.FormattersManager;
import org.openl.rules.testmethod.IParameterWithValueDeclaration;
import org.openl.rules.testmethod.TestUnitResultComparator.TestStatus;
import org.openl.rules.testmethod.result.ComparedResult;

import java.util.HashMap;
import java.util.Map;

public class ExpectedResultFilter extends AGridFilter {

    private Map<Point, ComparedResult> spreadsheetCellsForTest;

    public ExpectedResultFilter(Map<Point, ComparedResult> spreadsheetCellsForTest) {
        this.spreadsheetCellsForTest = new HashMap<Point, ComparedResult>(spreadsheetCellsForTest);
    }

    public FormattedCell filterFormat(FormattedCell cell) {
        Point cellCoordinates = new Point(cell.getAbsoluteColumn(), cell.getAbsoluteRow());

        if (spreadsheetCellsForTest.containsKey(cellCoordinates)) {
            ComparedResult result = spreadsheetCellsForTest.get(cellCoordinates);
            String formattedExpectedValue = null;
            boolean equals = (result.getStatus() == TestStatus.TR_OK);
            if (!equals) {
                Object expectedValue = result.getExpectedValue();
                if (expectedValue instanceof IParameterWithValueDeclaration) {
                    IParameterWithValueDeclaration declaration = (IParameterWithValueDeclaration) expectedValue;
                    expectedValue = declaration.getValue();
                }
                formattedExpectedValue = FormattersManager.format(expectedValue);
            }
            String image = getImage(result);
            String formattedResult = String.format(equals ? "%s %s" : "%s %s %s", image,
                    cell.getFormattedValue(), formattedExpectedValue);
            cell.setFormattedValue(formattedResult);
        }

        return cell;
    }

    private String getImage(ComparedResult result) {
        String image;
        if (result.getStatus().equals(TestStatus.TR_OK)) {
            image = "<i class=\"case-success\"></i>";
        } else {
            image = "<i class=\"case-error\"></i>";
        }
        return image;
    }

}
