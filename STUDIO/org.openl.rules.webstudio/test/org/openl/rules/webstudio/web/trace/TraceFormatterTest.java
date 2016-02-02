package org.openl.rules.webstudio.web.trace;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.openl.meta.DoubleValue;
import org.openl.rules.calc.Spreadsheet;
import org.openl.rules.calc.element.SpreadsheetCell;
import org.openl.rules.webstudio.web.trace.node.ATableTracerNode;
import org.openl.rules.webstudio.web.trace.node.SpreadsheetTracerLeaf;

public class TraceFormatterTest {

    @Test
    public void getDisplayNameTest() {
        Spreadsheet spreadsheet = createNodeMock();
        ATableTracerNode spreadsheetTraceObject = new ATableTracerNode("spreadsheet",
                "SpreadSheet",
                spreadsheet,
                null);
        SpreadsheetTracerLeaf leafNode = new SpreadsheetTracerLeaf(createCellMock());
        spreadsheetTraceObject.addChild(leafNode);

        leafNode.setResult(null);
        assertEquals("$Value$Vehicle_Premiums = null", TraceFormatter.getDisplayName(leafNode));

        leafNode.setResult(0.95);
        assertEquals("$Value$Vehicle_Premiums = 0.95", TraceFormatter.getDisplayName(leafNode));

        leafNode.setResult(new DoubleValue(0.95));
        assertEquals("$Value$Vehicle_Premiums = 0.95", TraceFormatter.getDisplayName(leafNode));

        leafNode.setResult(new DoubleValue[] { new DoubleValue(0.95) });
        assertEquals("$Value$Vehicle_Premiums = {0.95}", TraceFormatter.getDisplayName(leafNode));

        leafNode.setResult(new DoubleValue[] { new DoubleValue(0.95), new DoubleValue(0.55) });
        assertEquals("$Value$Vehicle_Premiums = {0.95,0.55}",
            TraceFormatter.getDisplayName(leafNode));

        leafNode.setResult(new double[][] { { 0.95, 0.55 }, { 1.95, 1.55 } });
        assertEquals("$Value$Vehicle_Premiums = {{0.95,0.55},{1.95,1.55}}",
            TraceFormatter.getDisplayName(leafNode));
    }

    protected SpreadsheetCell createCellMock() {
        SpreadsheetCell cell = mock(SpreadsheetCell.class);
        when(cell.getColumnIndex()).thenReturn(0);
        when(cell.getRowIndex()).thenReturn(0);
        return cell;
    }

    protected Spreadsheet createNodeMock() {
        Spreadsheet spreadsheet = mock(Spreadsheet.class);
        when(spreadsheet.getColumnNames()).thenReturn(new String[] { "Value" });
        when(spreadsheet.getRowNames()).thenReturn(new String[] { "Vehicle_Premiums" });
        return spreadsheet;
    }

}
