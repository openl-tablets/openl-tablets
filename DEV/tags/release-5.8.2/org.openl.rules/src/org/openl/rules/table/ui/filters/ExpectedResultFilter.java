package org.openl.rules.table.ui.filters;

import java.util.HashMap;
import java.util.Map;

import org.openl.rules.table.FormattedCell;
import org.openl.rules.table.Point;
import org.openl.rules.table.formatters.FormattersManager;
import org.openl.rules.testmethod.TestUnitResultComparator.TestStatus;
import org.openl.rules.testmethod.result.ComparedResult;
import org.openl.util.formatters.IFormatter;

public class ExpectedResultFilter extends AGridFilter {
    
    private Map<Point, ComparedResult> spreadsheetCellsForTest;
    
    public ExpectedResultFilter(Map<Point, ComparedResult> spreadsheetCellsForTest) {
        this.spreadsheetCellsForTest = new HashMap<Point, ComparedResult>(spreadsheetCellsForTest);
    }

    public FormattedCell filterFormat(FormattedCell cell) {
        Point cellCoordinates = new Point(cell.getAbsoluteColumn(), cell.getAbsoluteRow());
        
        if (spreadsheetCellsForTest.containsKey(cellCoordinates)) {
            ComparedResult result = spreadsheetCellsForTest.get(cellCoordinates);
            Object expectedValue = result.getExpectedValue();
            IFormatter formatter = FormattersManager.getFormatter(expectedValue);
            
            String image = getImage(result);
            cell.setFormattedValue(String.format("%s %s; %s", image, 
                cell.getFormattedValue(), formatter.format(expectedValue)));
        }
        
        return cell;       
    }

    private String getImage(ComparedResult result) {
        String image = null;
        if (result.getStatus().equals(TestStatus.TR_OK)) {
            image = "<img src=\"webresource/images/ok.png\"/>";
        } else {
            image = "<img src=\"webresource/images/error.png\"/>";
        }
        return image;
    }

}
