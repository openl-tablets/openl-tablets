package org.openl.rules.test.liveexcel.formula;

import static org.junit.Assert.*;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.junit.Test;
import org.openl.rules.liveexcel.formula.DeclaredFunctionSearcher;
import org.openl.rules.liveexcel.formula.DeclaredFunctionParser;
import org.openl.rules.liveexcel.formula.LiveExcelFunctionsPack;
import org.openl.rules.liveexcel.formula.LiveExcelFunctionsPack.UDFFinderLE;
import org.openl.rules.liveexcel.usermodel.LiveExcelWorkbook;
import org.openl.rules.liveexcel.usermodel.LiveExcelWorkbookFactory;

public class DeclareSearchTest {

    final Log LOGGER = LogFactory.getLog(DeclaredFunctionParser.class);

    @Test
    public void testDeclare() {
        Workbook workbook = getWorkbook();
        DeclaredFunctionSearcher searcher = new DeclaredFunctionSearcher(workbook);
        searcher.findFunctions();

        List<String> expectedFunctions = new ArrayList<String>();
        expectedFunctions.add("func1");
        expectedFunctions.add("func2");
        expectedFunctions.add("denisesFun");

        for (String functionName : expectedFunctions) {
            if (LiveExcelFunctionsPack.instance().getUDFFinderLE(workbook).findFunction(functionName) == null) {
                assertFalse(true);
            }
        }
        assertTrue(true);
    }

    @Test
    public void testFunction() {
        Workbook workbook = getWorkbook();
        DeclaredFunctionSearcher searcher = new DeclaredFunctionSearcher(workbook);
        Sheet sheet = workbook.getSheetAt(1);
        searcher.findFunctions();
        UDFFinderLE udfFinder = LiveExcelFunctionsPack.instance().getUDFFinderLE(workbook);
        FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator(udfFinder);
        Cell evaluateInCell = evaluator.evaluateInCell(sheet.getRow(13).getCell(1));
        Cell evaluateInCell2 = evaluator.evaluateInCell(sheet.getRow(2).getCell(3));
        Cell evaluateInCell3 = evaluator.evaluateInCell(sheet.getRow(2).getCell(4));
        assertTrue(20 == evaluateInCell.getNumericCellValue());
        assertTrue(25.5 == evaluateInCell2.getNumericCellValue());
        assertTrue(25 == evaluateInCell3.getNumericCellValue());
    }

    @Test
    public void testFunWithReturnFromOtherSheet() {
        Workbook workbook = getWorkbook();
        DeclaredFunctionSearcher searcher = new DeclaredFunctionSearcher(workbook);
        searcher.findFunctions();
        Sheet sheet = workbook.getSheetAt(2);
        FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator(
                LiveExcelFunctionsPack.instance().getUDFFinderLE(workbook));
        Cell evaluateInCell = evaluator.evaluateInCell(sheet.getRow(18).getCell(1));

        assertTrue(9 == evaluateInCell.getNumericCellValue());        
    }

//    @Test
//    public void testParse1() {
//        HSSFWorkbook workbook = getWorkbook();
//        String functionText = "ol_declare_function(" + '"' + "func1" + '"' + "," + '"' + "Computes total of its inputs"
//                + '"' + ", C8," + "" + '"' + "input one" + '"' + ", C8," + '"' + "input two" + '"' + ", C5," + '"'
//                + "input three" + '"' + ", C6)";
//        ParsedDeclaredFunction parsFunc = DeclaredFunctionParser.parseFunction(functionText, workbook);
//
//        assertTrue("func1".equals(parsFunc.getDeclFuncName()));
//        assertTrue("Computes total of its inputs".equals(parsFunc.getDescription()));
//        assertTrue("C8".equals(parsFunc.getReturnCell().toFormulaString()));
//        assertTrue(parsFunc.getParameters().size() == 3);
//        assertTrue(parsFunc.getParameters().get(0).getParamName().equals("input one"));
//        assertTrue(parsFunc.getParameters().get(1).getParamName().equals("input two"));
//        assertTrue(parsFunc.getParameters().get(2).getParamName().equals("input three"));
//    }
//
//    @Test
//    public void testParse2() {
//        HSSFWorkbook workbook = getWorkbook();
//        String functionText = "ol_declare_function(" + '"' + "func2" + '"' + ", C111, C5," + '"' + "input two" + '"'
//                + ",C6,C7)";
//        ParsedDeclaredFunction parsFunc = DeclaredFunctionParser.parseFunction(functionText, workbook);
//
//        assertTrue("func2".equals(parsFunc.getDeclFuncName()));
//        assertTrue(parsFunc.getDescription() == null);
//        assertTrue("C111".equals(parsFunc.getReturnCell().toFormulaString()));
//        assertTrue(parsFunc.getParameters().get(0).getParamName().equals(""));
//        assertTrue(parsFunc.getParameters().get(1).getParamName().equals("input two"));
//        assertTrue(parsFunc.getParameters().get(2).getParamName().equals(""));
//    }
//
//    @Test
//    public void testParse3() {
//        HSSFWorkbook workbook = getWorkbook();
//        String functionText = "ol_declare_function(" + '"' + "func3" + '"' + ", C122)";
//        ParsedDeclaredFunction parsFunc = DeclaredFunctionParser.parseFunction(functionText, workbook);
//
//        assertTrue("func3".equals(parsFunc.getDeclFuncName()));
//        assertTrue(parsFunc.getDescription() == null);
//        assertTrue("C122".equals(parsFunc.getReturnCell().toFormulaString()));
//    }

    private Workbook getWorkbook() {
        String fileName = "./test/resources/Functions_2009.05.18.xls";
        LiveExcelWorkbook workbook = null;
        InputStream is = null;
        try {
            is = new FileInputStream(fileName);
            workbook = LiveExcelWorkbookFactory.create(is, "SimpleExample");
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InvalidFormatException e) {            
            e.printStackTrace();
        } finally {
            try {
                if (is != null)
                    is.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return workbook;
    }
}
