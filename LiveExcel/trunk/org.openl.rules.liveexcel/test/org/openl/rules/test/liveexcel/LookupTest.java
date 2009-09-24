package org.openl.rules.test.liveexcel;

import java.io.FileInputStream;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.ErrorConstants;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.junit.Test;
import org.openl.rules.liveexcel.formula.DeclaredFunctionSearcher;
import org.openl.rules.liveexcel.formula.LiveExcelFunctionsPack;
import org.openl.rules.liveexcel.formula.LiveExcelFunctionsPack.UDFFinderLE;
import org.openl.rules.liveexcel.usermodel.LiveExcelWorkbookFactory;

import static org.junit.Assert.*;

public class LookupTest {
    @Test
    public void test() throws Exception {
        Workbook workbook = LiveExcelWorkbookFactory.create(new FileInputStream("./test/resources/LookupTest.xls"),
                null);
        new DeclaredFunctionSearcher(workbook).findFunctions();
        Sheet sheet = workbook.getSheetAt(0);
        Cell cell = sheet.getRow(11).getCell(0);
        Cell cell2 = sheet.getRow(12).getCell(0);
        Cell cell3ExpanionData = sheet.getRow(13).getCell(0);
        Cell cell4Horizontal = sheet.getRow(14).getCell(0);
        Cell cell5Vertical = sheet.getRow(15).getCell(0);
        Cell cell6Skipped = sheet.getRow(16).getCell(0);
        Cell cell7NoValue = sheet.getRow(17).getCell(0);

        UDFFinderLE udfFinde = LiveExcelFunctionsPack.instance().getUDFFinderLE(workbook);
        FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator(udfFinde);
        assertTrue(1 == evaluator.evaluate(cell).getNumberValue());
        assertTrue(0.965 == evaluator.evaluate(cell2).getNumberValue());
        assertTrue(0.985 == evaluator.evaluate(cell3ExpanionData).getNumberValue());
        assertTrue(0.945 == evaluator.evaluate(cell4Horizontal).getNumberValue());
        assertTrue(1.001 == evaluator.evaluate(cell5Vertical).getNumberValue());
        assertTrue(3 == evaluator.evaluate(cell6Skipped).getNumberValue());
        assertTrue(ErrorConstants.ERROR_NA == evaluator.evaluate(cell7NoValue).getErrorValue());
    }

}
