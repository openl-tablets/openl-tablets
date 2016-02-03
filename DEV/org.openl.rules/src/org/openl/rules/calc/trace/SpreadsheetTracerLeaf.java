package org.openl.rules.calc.trace;

import org.openl.rules.calc.element.SpreadsheetCell;
import org.openl.rules.method.ExecutableRulesMethod;
import org.openl.rules.table.ATableTracerNode;

/**
 * Leaf trace object that represented by one calculation of spreadsheet cell
 *
 * @author PUdalau
 */
public class SpreadsheetTracerLeaf extends ATableTracerNode {
    private SpreadsheetCell spreadsheetCell;

    public SpreadsheetTracerLeaf(ExecutableRulesMethod method, SpreadsheetCell spreadsheetCell) {
        super("spreadsheetCell", null, method, null);
        this.spreadsheetCell = spreadsheetCell;
    }

    public SpreadsheetCell getSpreadsheetCell() {
        return spreadsheetCell;
    }
}
