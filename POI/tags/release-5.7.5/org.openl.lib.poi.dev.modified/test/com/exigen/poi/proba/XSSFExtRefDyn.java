/**
 * 
 */
package com.exigen.poi.proba;
import java.io.IOException;
import java.io.InputStream;

import junit.framework.TestCase;


import org.apache.poi.hssf.record.formula.eval.NumberEval;
import org.apache.poi.hssf.record.formula.eval.ValueEval;
import org.apache.poi.ss.formula.eval.forked.ForkedEvaluator;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.Test;


/**
 * @author vabramovs
 *
 */
public class XSSFExtRefDyn  extends TestCase{

	/**
	 * @param args
	 */
	@Test
	public void testExternalWorkbooks(){
        try {
        	InputStream inp = XSSFExtRefDyn.class.getClassLoader().getResourceAsStream("ExtLinkA.xlsx");
			Workbook bookA = new XSSFWorkbook(inp);
			
			ForkedEvaluator feA = ForkedEvaluator.create(bookA, null,null);
			ForkedEvaluator.setupEnvironment( new String[] {"ExtLinkA.xlsx"}, new ForkedEvaluator[]{feA,});
			ValueEval eval = feA.evaluate("Sheet1", 1, 1);  //B2
			assertEquals(((NumberEval)eval).getNumberValue(),3.0,0);
			System.out.println("Result="+eval);
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

}

