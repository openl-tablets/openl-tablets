package org.openl.rules.webstudio.web.trace.node;

import org.openl.rules.calc.element.SpreadsheetCell;

/**
 * Leaf trace object that represented by one calculation of spreadsheet cell
 *
 * @author PUdalau
 */
public class SpreadsheetTracerLeaf extends ATableTracerNode {
    private final SpreadsheetCell spreadsheetCell;

    SpreadsheetTracerLeaf(SpreadsheetCell spreadsheetCell) {
        super("spreadsheetCell", null, null, null);
        this.spreadsheetCell = spreadsheetCell;
    }

    public SpreadsheetCell getSpreadsheetCell() {
        return spreadsheetCell;
    }
}
