package com.exigen.le.evaluator;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.poi.hssf.record.formula.eval.BoolEval;
import org.apache.poi.hssf.record.formula.eval.ErrorEval;
import org.apache.poi.hssf.record.formula.eval.NumberEval;
import org.apache.poi.hssf.record.formula.eval.StringEval;
import org.apache.poi.hssf.record.formula.eval.ValueEval;
import org.apache.poi.hssf.record.formula.udf.UDFFinder;
import org.apache.poi.ss.formula.ArrayEval;
import org.apache.poi.ss.formula.EvaluationCell;
import org.apache.poi.ss.formula.IExternalWorkbookResolver;
import org.apache.poi.ss.formula.WorkbookEvaluator;
import org.apache.poi.ss.formula.eval.forked.ForkedEvaluator;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.ArrayFormulaEvaluatorHelper;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Workbook;

import com.exigen.le.smodel.Function;
import com.exigen.le.smodel.ServiceModel;
import com.exigen.le.usermodel.LiveExcelWorkbook;

/**
 * Evaluator for any declared LiveExcel function.
 * 
 * @author PUdalau
 */
/**
 * @author vabramovs
 *
 */
public class LiveExcelEvaluator {
	private static final Log LOG = LogFactory.getLog(LiveExcelEvaluator.class);

    private Workbook workbook;
    private IExternalWorkbookResolver externalResolver;

    /**
     * Parse workbook and create LiveExcelEvaluator.
     * 
     * @param workbook Workbook to parse.
     * @param evaluationContext EvaluationContext associated with evaluator.
     */
    public LiveExcelEvaluator(Workbook workbook, IExternalWorkbookResolver externalResolver ) {
        this.workbook = workbook;
        this.externalResolver = externalResolver;
    }


 
    /**
     * Evaluates any declared function with specified arguments
     * 
     * @param functionName Name of function.
     * @param args Arguments for function.
     * @return Result of evaluation.
     */

    public ValueEval evaluateServiceModelUDF(String functionName, Object[] args) {
    	
    	LOG.debug("Start to evaluateServiceModelUDF:"+functionName);
        ForkedEvaluator evaluator = createEvaluator();
        
 		evaluator.createEnvironment();
 		
        ValueEval[] processedArgs = new ValueEval[args.length];
        for (int i = 0; i < args.length; i++) {
            processedArgs[i] = createEvalForObject(args[i], evaluator.getWorkbookEvaluator(),ThreadEvaluationContext.getDataPool());
        }
        
        ServiceModel sm = ThreadEvaluationContext.getServiceModel();
        for(Function func: sm.getFunctions()){
        	if(func.getName().equalsIgnoreCase(functionName)){
        		com.exigen.le.smodel.Cell returnCell  = func.obtainReturnSpace().from();
        		String sheetName = returnCell.getSheetName();
        		if(sheetName.isEmpty())
        			sheetName = func.getSheet();
        		int rowIndex = returnCell.getRowIndex();
        		int colIndex = returnCell.getColumnIndex();
        		
        		EvaluationCell cell;
				try {
					cell = evaluator.getEvaluationCell(sheetName, rowIndex, colIndex);
				} catch (Exception e) {
					com.exigen.le.smodel.Cell c = new com.exigen.le.smodel.Cell();
					c.from().setSheetName(sheetName);
					c.from().setColumnIndex(colIndex);
					c.from().setRow(rowIndex+1);
					
					String msg = "Could not get resulting cell "+c.from().toString()+" for function "+functionName;
					LOG.error(msg,e);
					throw new RuntimeException(msg);
				}
        		
        		ValueEval result = evaluator.getWorkbookEvaluator().calculate(functionName,cell, processedArgs);
        		evaluator.disposeEnvironment();
            	LOG.debug("Result of evaluateServiceModelUDF:"+functionName+" is "+result.toString());
               
               return result;
        	}
        }
        String msg = " Could not find UDF "+ functionName;
        LOG.error(msg);
        throw new RuntimeException(msg);
    }
//    private void finalizeEvaluation(ForkedEvaluator evaluator, Cell formulaCell) {
//        removeExecutedCell(formulaCell);
//    }

    private ForkedEvaluator createEvaluator() {
        UDFFinder finder = ((LiveExcelWorkbook)workbook).getUDFFinder();
        ForkedEvaluator evaluator = ForkedEvaluator.create(workbook, null, finder);
        evaluator.getWorkbookEvaluator().setResolver(externalResolver);
        return evaluator;
    }
    
    /**
     * @return Cell used for execution of new formula
     */
    private synchronized Cell findEmptyCell() {
        int lastRowIndex = workbook.getSheetAt(0).getLastRowNum();
        
        Row row = workbook.getSheetAt(0).createRow(lastRowIndex + 1);
        return row.createCell(0);
    }

    private void removeExecutedCell(Cell cell) {
        workbook.getSheetAt(0).removeRow(cell.getRow());
    }

    /**
     * Generates string representation of formula for cell by function name and arguments.
     */
    private String createFormula(String functionName, ValueEval[] args) {
        StringBuffer formula = new StringBuffer(functionName + "(");
        for (int i = 0; i < args.length; i++) {
            formula.append(extractStringValueFromValueEval(args[i]));
            if (i < args.length - 1) {
                formula.append(',');
            }
        }
        formula.append(')');
        return formula.toString();
    }

    private String extractStringValueFromValueEval(ValueEval eval) {
        return ArrayFormulaEvaluatorHelper.evalToCellValue(eval).formatAsString();
    }

    private static StringEval addObjectToPool(Object object, DataPool pool) {
        return new StringEval(pool.add(object));
    }

   /**
    * Convert Object to POI ValueEval
 * @param object
 * @param evaluator
 * @param pool
 * @return
 */
public static ValueEval createEvalForObject(Object object, WorkbookEvaluator evaluator, DataPool pool) {
    	if(object instanceof ValueEval){  // already ValueEval
    		return (ValueEval)object;
    	}else if (object instanceof Number) {
            return new NumberEval(((Number) object).doubleValue());
        } else if (object instanceof Boolean) {
            return BoolEval.valueOf((Boolean) object);
        } else if (object instanceof String) {
            return interpretString((String) object);
        } else if (object instanceof Calendar) {
            return new NumberEval(DateUtil.getExcelDate((Calendar) object, false));
        } else if (object instanceof Date) {
            return new NumberEval(DateUtil.getExcelDate((Date) object));
        } else if (object instanceof Collection<?>) {
            return createEvalForCollection((Collection<?>)object, evaluator,pool);
        }  else  {  
        	if(object.getClass().getComponentType() != null) { // Array
        		List<?> list = Arrays.asList((Object[])object);
                return createEvalForCollection(list, evaluator,pool);
        	}
        	else {
        		return addObjectToPool(object, pool);
        	}
        }

    }
   /**
    * Try to interpret String value as other type:
    * if first letter is "'" - always string without this first letter
    * If string can be converted to double - return NumericEval
    * If string contains "true" or "false" ignore case - return BoolEval
    * Otherwise - return StringEval with input string 
 * @param str
 * @return
 */
static private ValueEval interpretString(String str){
	   if(str.charAt(0)=="'".charAt(0)){
		   return new StringEval(str.substring(1));
	   }
	   try {
		Double doub = Double.parseDouble(str);
		return new NumberEval(doub);
	} catch (NumberFormatException e) {
		Boolean bool = Boolean.parseBoolean(str);
		if(!bool){
			if(!str.equalsIgnoreCase("false")){
				return new StringEval(str);
			}
		}
		return BoolEval.valueOf(bool);
	}
 }
   
   /**
    * Create Object from ValueEval (including ArrayEval)
 * @param value
 * @param evaluator
 * @param pool
 * @return
 */
public static Object createObjectForEval(ValueEval value, WorkbookEvaluator evaluator, DataPool pool) {
       if (value instanceof ArrayEval) {
           return create2DArray((ArrayEval)value, evaluator,pool);
       }  else  {
       	return createObjectForEval(value);
       }

   }
   /**
    * Create Object from ValueEval (not support ArrayEval)
    * new code must use createObjectForEval(ValueEval value, WorkbookEvaluator evaluator, DataPool pool)
    * @deprecated - new code must use createObjectForEval(ValueEval value, WorkbookEvaluator evaluator, DataPool pool)
    * @param value
    * @return
    */
public static Object createObjectForEval(ValueEval value){
       if (value instanceof NumberEval) {
           return new Double(((NumberEval) value).getNumberValue());
       } else if (value instanceof BoolEval) {
           return new Boolean(((BoolEval) value).getBooleanValue());
       } else if (value instanceof StringEval) {
           return new String(((StringEval) value).getStringValue());
       } else if (value instanceof ErrorEval) {
           return new String(((ErrorEval) value).toString());
       }    else  {
       	String msg = "Unsupported type of ValueEval:"+value.getClass().getSimpleName();
       	LOG.error(msg);
       	throw new RuntimeException(msg);
       }

   }

    private static Object create2DArray(ArrayEval value,
			WorkbookEvaluator evaluator, DataPool pool) {
    		Object[][] result = new Object[value.getHeight()][value.getWidth()];
    		ValueEval[][]array = value.getArrayValues();
    		for(int i=0;i<value.getHeight();i++){
    			for(int j=0;j<value.getWidth();j++){
    				result[i][j]= createObjectForEval(array[i][j],evaluator,pool);
    			}
    		}
 		return result;
	}

	private static boolean isPrimitive(Object object) {
        if (object instanceof Number) {
            return true;
        } else if (object instanceof Boolean) {
            return true;
        } else if (object instanceof String) {
            return true;
        } else if (object instanceof Calendar) {
            return true;
        }  else  {
            return false;
        }

    }
    private static ValueEval createEvalForCollection(Collection<?> collection, WorkbookEvaluator evaluator,DataPool pool) {
    	Iterator<?> it = collection.iterator();
    	Object element = it.next();
    	if(isPrimitive(element)){
    		ValueEval[][] values = new ValueEval[collection.size()][1];
    		for(int i=0;i<collection.size();i++){
    			values[i][0]=createEvalForObject(element, evaluator, pool);
    			if(it.hasNext()){
      				element = it.next();
    			}
     		}
    		
    		return new ArrayEval(values);	
    	}
    	else   	if(element.getClass().getComponentType() != null) { // 2D Array
    		Object[]objects= (Object[])element;
       		ValueEval[][] values = new ValueEval[collection.size()][objects.length];
       		for(int i=0;i<collection.size();i++){
          		for(int j=0;j<objects.length;j++){
           			values[i][j]=createEvalForObject(objects[j], evaluator, pool);
          		}
      			if(it.hasNext()){
      				element = it.next();
         	  		objects= (Object[])element;
 	    			}
       		}
       		return new ArrayEval(values);	
    	}
    	else {
            return addObjectToPool(collection, pool);
    	}
    	
    }

}
