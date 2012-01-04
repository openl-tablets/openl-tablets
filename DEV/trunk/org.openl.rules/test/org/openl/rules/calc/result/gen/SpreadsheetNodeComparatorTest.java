package org.openl.rules.calc.result.gen;

import static org.junit.Assert.*;

import org.junit.Test;
import org.openl.rules.BaseOpenlBuilderHelper;

public class SpreadsheetNodeComparatorTest extends BaseOpenlBuilderHelper {
    
    private static String __src = "test/rules/calc1/CustomSpreadsheetInSpreadsheet.xls";

    public SpreadsheetNodeComparatorTest() {
        super(__src);
    }
    
    @Test
    public void testNoErrors() {
        // checks that no errors falls on compilation
        // Inside there is a usage of custom spreadsheet result in the other spreadsheet
        //
        assertNotNull(getJavaWrapper().getCompiledClass().getOpenClass());
    }
}
