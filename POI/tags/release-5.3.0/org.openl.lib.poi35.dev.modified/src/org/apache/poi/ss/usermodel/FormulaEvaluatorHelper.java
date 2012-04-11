/**
 * 
 */
package org.apache.poi.ss.usermodel;

import org.apache.poi.hssf.record.formula.eval.BoolEval;
import org.apache.poi.hssf.record.formula.eval.ErrorEval;
import org.apache.poi.hssf.record.formula.eval.NumberEval;
import org.apache.poi.hssf.record.formula.eval.StringEval;
import org.apache.poi.hssf.record.formula.eval.ValueEval;
import org.apache.poi.ss.formula.ArrayFormula;

/**
 * This class contains methods, common for HSSF and XSSF FormulaEvaluator
 *   Better solution - to have abstract class for FormulaEvaluator Interface Implementation
 *  All method need to be static 
 * @author vabramovs(VIA)
 *
 */
public class FormulaEvaluatorHelper {
	
	private FormulaEvaluatorHelper (){
		// has no instance
	}
	
	
	/** Transform Value according aimed Range
	 * @param cvs
	 * @param af
	 * @return
	 */
	public static Object[][] transform2Range(Object[][] cvs,ArrayFormula af){
		
		Object[][] answer = null;
		if(cvs[0][0] instanceof CellValue)
			answer = new CellValue[af.getLastRow()-af.getFirstRow()+1][af.getLastColumn()-af.getFirstColumn()+1];
		else if (cvs[0][0] instanceof ValueEval)
			answer = new ValueEval[af.getLastRow()-af.getFirstRow()+1][af.getLastColumn()-af.getFirstColumn()+1];
		else
			throw new RuntimeException("transform2Range does not support type "+cvs[0][0].getClass().getName());
		int rowStart = af.getFirstRow();
		int colStart = af.getFirstColumn();
		for(int i=rowStart;i<=af.getLastRow();i++ )
			for(int j=colStart; j<=af.getLastColumn();j++)
			{
				if((i-rowStart)<cvs.length && (j-colStart)<cvs[i-rowStart].length){
					answer[i-rowStart][j-colStart] = cvs[i-rowStart][j-colStart];
				}
				else
				{  
					boolean needClone = false;
					int cloneRow =  0;
					int cloneCol = 0;
					if(cvs.length == 1)
					{  // Need to clone first colm of  cvs
						cloneCol = j-colStart;
						needClone = true;
						
					}
					if(cvs[0].length == 1 )
					{  // Need to clone first row of  cvs
						cloneRow = i-rowStart;
						needClone = true;
						
					}
					if(needClone &&  cloneCol <cvs[0].length && cloneRow <cvs.length) 
					{
						
						answer[i-rowStart][j-colStart] = cvs[cloneRow][cloneCol];
					}	
					else 
					{
						//  For other cases set cell value to #N/A
						// For those cells we changes also their type to Error
						if(cvs[0][0] instanceof CellValue){
								CellValue cvError = CellValue.getError(org.apache.poi.ss.usermodel.ErrorConstants.ERROR_NA);
								answer[i-rowStart][j-colStart] = cvError;
						}	
						else 
							 if (cvs[0][0] instanceof ValueEval)
								 answer[i-rowStart][j-colStart] = ErrorEval.NA;
					}
				}	
			}
		return answer;
	}
	/**
	 * Convert Eval value to CellValue
	 * @param val
	 * @return
	 */
	public static CellValue eval2Cell(ValueEval val){
		if(val instanceof BoolEval)
			return CellValue.valueOf(((BoolEval)val).getBooleanValue());
		if(val instanceof NumberEval)
			return  new CellValue(((NumberEval)val).getNumberValue());
		if(val instanceof StringEval)
			return  new CellValue(((StringEval)val).getStringValue());
		if(val instanceof ErrorEval)
			return  new CellValue(((ErrorEval)val).getErrorCode());
		return new CellValue(ErrorEval.VALUE_INVALID.getErrorCode());
	}

}
