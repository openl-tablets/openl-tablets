package org.openl.rules.test.liveexcel;

import java.io.FileInputStream;
import java.util.Set;

import org.apache.poi.hssf.record.formula.eval.StringEval;
import org.apache.poi.hssf.record.formula.eval.ValueEval;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.ErrorConstants;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.junit.Test;
import org.openl.rules.liveexcel.LiveExcelEvaluator;
import org.openl.rules.liveexcel.formula.DeclaredFunctionSearcher;
import org.openl.rules.liveexcel.formula.LiveExcelFunctionsPack;
import org.openl.rules.liveexcel.formula.LiveExcelFunctionsPack.UDFFinderLE;
import org.openl.rules.liveexcel.usermodel.LiveExcelWorkbook;
import org.openl.rules.liveexcel.usermodel.LiveExcelWorkbookFactory;

import static org.junit.Assert.*;

public class BigLookupTest {
    
	
	
	
	@Test
    public void test() throws Exception {
        LiveExcelWorkbook workbook = LiveExcelWorkbookFactory.create(new FileInputStream("e:/temp/biglookup.xls"), null /*"SimpleExample"*/);
        new DeclaredFunctionSearcher(workbook).findFunctions();

        
        LiveExcelEvaluator evaluator = new LiveExcelEvaluator(workbook, workbook.getEvaluationContext());
     
        UDFFinderLE udfFinde = LiveExcelFunctionsPack.instance().getUDFFinderLE(workbook);
        Set<String> funcs = udfFinde.getUserDefinedFunctionNames();
        for(String func:funcs){
        	System.out.println("Find function:"+func);
        }

        Integer[] args = {new Integer(35),new Integer(7)};
        
		ValueEval res = evaluator.evaluateServiceModelUDF("BigLookup", args);
		assertTrue(((StringEval)res).getStringValue().equals("r35c7"));
      
		System.out.println("Res="+ ((StringEval)res).getStringValue());
		
//    	LiveExcelWorkbook workbook = LiveExcelWorkbookFactory.create(new FileInputStream("./test/resources/BigLookupTest.xls"),
//                null);
//        new DeclaredFunctionSearcher(workbook).findFunctions();
//        Sheet sheet = workbook.getSheetAt(0);
//        Cell cell = sheet.createRow(2).createCell(0);
//        cell.setCellFormula("BigLookup(59,7)");
//        System.out.println("Set formula-"+cell.getCellFormula());
//
//        UDFFinderLE udfFinde = LiveExcelFunctionsPack.instance().getUDFFinderLE(workbook);
//        FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator(udfFinde);
//        assertTrue(evaluator.evaluate(cell).getStringValue().equals("r59c7"));
//        
//      System.out.println("Res="+ evaluator.evaluate(cell).getStringValue());

    }

}
