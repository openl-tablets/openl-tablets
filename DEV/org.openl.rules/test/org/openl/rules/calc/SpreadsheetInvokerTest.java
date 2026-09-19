package org.openl.rules.calc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;

import org.openl.rules.calc.element.SpreadsheetCell;

class SpreadsheetInvokerTest {

    @Test
    void answersWithAnEmptyResultWhenTheSpreadsheetHasNoCells() {
        var spreadsheet = mock(Spreadsheet.class);
        when(spreadsheet.getCells()).thenReturn(new SpreadsheetCell[0][]);
        // a height that outruns the cells used to walk the invoker off the end of the array
        when(spreadsheet.getHeight()).thenReturn(2);
        when(spreadsheet.getWidth()).thenReturn(2);

        var invoker = new SpreadsheetInvoker(spreadsheet);

        assertEquals(0, invoker.preFetchedResult.length);
    }
}
