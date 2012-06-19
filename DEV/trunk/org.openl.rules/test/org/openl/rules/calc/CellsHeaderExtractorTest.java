package org.openl.rules.calc;

import static org.junit.Assert.*;

import org.junit.Test;

public class CellsHeaderExtractorTest {

    @Test
    public void testCSRDependencyRegex() {
        String cellName1 = "ColumnName : SpreadsheetResulttest";
        assertTrue(cellName1.matches(CellsHeaderExtractor.DEPENDENT_CSR_REGEX));

        String cellName2 = "ColumnName : SpreadsheetResult[]";
        assertFalse(cellName2.matches(CellsHeaderExtractor.DEPENDENT_CSR_REGEX));

        String cellName3 = "ColumnName : SpreadsheetResult";
        assertFalse(cellName3.matches(CellsHeaderExtractor.DEPENDENT_CSR_REGEX));
        
        String cellName4 = "ColumnName : SpreadsheetResult sdfsdf";
        assertFalse(cellName4.matches(CellsHeaderExtractor.DEPENDENT_CSR_REGEX));
    }
}
