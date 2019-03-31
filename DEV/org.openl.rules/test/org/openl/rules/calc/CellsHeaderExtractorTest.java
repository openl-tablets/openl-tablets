package org.openl.rules.calc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;

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

    @Test
    public void testGetSignatureDependencies() {
        List<String> dependencies = CellsHeaderExtractor.getSignatureDependencies(
            "Spreadsheet SpreadsheetResultCalcForTests_v10 calcTotalsTrace(TestHelperDataBean_v10 testdata)");
        assertEquals(Arrays.asList("CalcForTests_v10"), dependencies);

        dependencies = CellsHeaderExtractor.getSignatureDependencies(
            "Spreadsheet SpreadsheetResultCalcForTests calcTotalsTrace(SpreadsheetResultParam1 p1,int p2,SpreadsheetResultParam3 SpreadsheetResult3)");
        assertEquals(Arrays.asList("CalcForTests", "Param1", "Param3"), dependencies);

        dependencies = CellsHeaderExtractor.getSignatureDependencies(
            "Spreadsheet SpreadsheetResult calcTotalsTrace ( SpreadsheetResult SpreadsheetResult1, SpreadsheetResultParam2[] p2 )");
        assertEquals(Arrays.asList("Param2"), dependencies);

        dependencies = CellsHeaderExtractor
            .getSignatureDependencies("Spreadsheet SpreadsheetResultCalcForTests[] calcTotalsTrace");
        assertEquals(Arrays.asList("CalcForTests"), dependencies);

        dependencies = CellsHeaderExtractor
            .getSignatureDependencies("Spreadsheet ASpreadsheetResultCalcForTests calcTotalsTrace");
        assertTrue(dependencies.isEmpty());
    }
}
