package org.openl.rules.test.liveexcel;

import org.apache.poi.hssf.usermodel.HSSFFormulaEvaluator;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.junit.Test;
import org.openl.rules.liveexcel.formula.DeclaredFunctionSearcher;
import org.openl.rules.liveexcel.hssf.usermodel.LiveExcelHSSFWorkbook;
import org.openl.rules.test.liveexcel.formula.PerformanceAndThreadSafetyTest;

import static org.junit.Assert.*;

public class LookupTest {
    @Test
    public void test() {
        Workbook workbook = PerformanceAndThreadSafetyTest.getHSSFWorkbook("./test/resources/LookupTest.xls");
        new DeclaredFunctionSearcher(workbook).findFunctions();
        Sheet sheet = workbook.getSheetAt(0);
        Cell cell = sheet.getRow(11).getCell(0);
        Cell cell2 = sheet.getRow(12).getCell(0);
        Cell cell3ExpanionData = sheet.getRow(13).getCell(0);
        Cell cell4Horizontal = sheet.getRow(14).getCell(0);
        Cell cell5Vertical = sheet.getRow(15).getCell(0);
        Cell cell6Skipped = sheet.getRow(16).getCell(0);
        HSSFFormulaEvaluator.evaluateAllFormulaCells((LiveExcelHSSFWorkbook) workbook);
        assertTrue(1 == cell.getNumericCellValue());
        assertTrue(0.965 == cell2.getNumericCellValue());
        assertTrue(0.985 == cell3ExpanionData.getNumericCellValue());
        assertTrue(0.945 == cell4Horizontal.getNumericCellValue());
        assertTrue(1.001 == cell5Vertical.getNumericCellValue());
        assertTrue(3 == cell6Skipped.getNumericCellValue());
    }

}
