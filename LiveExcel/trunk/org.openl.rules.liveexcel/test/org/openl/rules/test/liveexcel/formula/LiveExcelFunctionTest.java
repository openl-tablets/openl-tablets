package org.openl.rules.test.liveexcel.formula;

import static org.junit.Assert.*;

import org.junit.Test;
import org.openl.rules.test.liveexcel.formula.ExcelFunctionCalculator.CellAddress;

public class LiveExcelFunctionTest {

    @Test
    public void testUDF() {
        ExcelFunctionCalculator calculator = new ExcelFunctionCalculator("./test/resources/FOO.xls");
        Object[] inputValues = new Object[] { 10 };
        CellAddress[] inputCellAddresses = new CellAddress[] { new CellAddress("B1") };
        assertEquals("The result of our UDF is", 3, calculator.calculateResult(new CellAddress("B2"), inputValues,
                inputCellAddresses));
    }
}
