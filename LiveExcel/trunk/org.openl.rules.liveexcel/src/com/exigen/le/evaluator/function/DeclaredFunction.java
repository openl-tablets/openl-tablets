package com.exigen.le.evaluator.function;



import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.poi.hssf.record.formula.eval.ErrorEval;
import org.apache.poi.hssf.record.formula.eval.RefEval;
import org.apache.poi.hssf.record.formula.eval.StringEval;
import org.apache.poi.hssf.record.formula.eval.ValueEval;
import org.apache.poi.hssf.record.formula.functions.FreeRefFunction;
import org.apache.poi.ss.formula.ArrayEval;
import org.apache.poi.ss.formula.EvaluationCell;
import org.apache.poi.ss.formula.IExternalWorkbookResolver;
import org.apache.poi.ss.formula.OperationEvaluationContext;
import org.apache.poi.ss.formula.WorkbookEvaluator;
//import org.apache.poi.ss.usermodel.Cell;

import com.exigen.le.evaluator.DataPool;
import com.exigen.le.evaluator.LiveExcelEvaluator;
import com.exigen.le.evaluator.ThreadEvaluationContext;
import com.exigen.le.evaluator.function.TypifiedRange.TypifiedRange;
import com.exigen.le.project.ProjectLoader;
import com.exigen.le.smodel.Cell;
import com.exigen.le.smodel.ExcelSpace;
import com.exigen.le.smodel.Function;
import com.exigen.le.smodel.MappedProperty;
import com.exigen.le.smodel.Range;
import com.exigen.le.smodel.SMHelper;
import com.exigen.le.smodel.ServiceModel;
import com.exigen.le.smodel.Type;
import com.exigen.le.smodel.Function.FunctionArgument;

/**
 * DeclaredFunction - handles parsed ol_declare_function
 * 
 */
public class DeclaredFunction implements FreeRefFunction  {

	private static final Log LOG = LogFactory.getLog(DeclaredFunction.class);

    private String funcName;
    
    public DeclaredFunction(String  funcName){
    	this.funcName = funcName;	

		
    }
	public ValueEval evaluate(ValueEval[] args, OperationEvaluationContext ec) {
		LOG.debug("Invoke function "+funcName);
		ServiceModel sm = ThreadEvaluationContext.getServiceModel();
		
	    Function funcDesc = ThreadEvaluationContext.getFunctionSelector().selectFunction(funcName, sm.getFunctions(), ThreadEvaluationContext.getInstance());
		
		WorkbookEvaluator wbEvaluator = ec.getWorkbookEvaluator();
		DataPool pool = ThreadEvaluationContext.getDataPool();
		//FIXME: workbook comparsion: now in compares by  "==", and works only because cache of workbooks is used in ProjectLoader 
        if (!ProjectLoader.getWorkbook(ThreadEvaluationContext.getProject(), funcDesc.getExcel()).equals(
                ec.getWorkbook().getWorkbook())) {
			IExternalWorkbookResolver resolver = ec.getWorkbookEvaluator().getResolver();
			LiveExcelEvaluator evaluator;
            try {
                evaluator = new LiveExcelEvaluator(resolver.resolveExternalWorkbook(funcDesc.getExcel()), resolver);
                LOG.debug("Function "+funcName+" is defined in other workbook "+funcDesc.getExcel());
                return evaluator.evaluateServiceModelUDF(funcName, args);
            } catch (Exception e) {
                LOG.error("Can not find workbook " + funcDesc.getExcel() + " that contains function " + funcName,e);
                return ErrorEval.REF_INVALID;
            }
		}
        if (args.length != funcDesc.getArguments().size()) {
    		LOG.debug("Wrong arguments count for function "+funcName+". Expected "+funcDesc.getArguments().size()+", but was "+args.length);
            return ErrorEval.VALUE_INVALID;
        } else {
    		// Resolve all reference before calculating
            for (int i = 0; i < args.length; i++) {
        		// Resolve all reference before calculating
    	    	if(args[i] instanceof RefEval){
    	    		args[i]= ((RefEval)args[i]).getInnerValueEval();
    	    	}
            }
            //  organize mosaic
            Loops loops = new Loops(funcDesc.getArguments(),args,wbEvaluator, pool);
            ValueEval result = null; 
            ValueEval[][] array=null;
            for(int ii= 0;ii<loops.rowCount;ii++){
                for(int jj= 0;jj<loops.colCount;jj++){
                	System.out.println("Function "+funcDesc.getName()+",loop="+ii+","+jj);
                	ValueEval loopResult = calculateOneLoop(wbEvaluator,funcDesc, args,ii,jj, ec, pool);
                	if(loops.rowCount==1 && loops.colCount==1){
                		result = loopResult;
                		break;
                	}
                	else {
                		if(result == null){
                			array = new ValueEval[loops.rowCount][loops.colCount];
                			result = new ArrayEval(array);
                		}
                		array[ii][jj]=loopResult;
                	}
                }
            }
            	
            return result;
        }
	}
	private ValueEval calculateOneLoop(WorkbookEvaluator wbEvaluator,Function funcDesc,ValueEval[] args,int ii,int jj,OperationEvaluationContext ec, DataPool pool){
    	String sheetName;
        for (int i = 0; i < args.length; i++) {
        	FunctionArgument arg = funcDesc.getArguments().get(i);
        	sheetName = arg.obtainInput().from().getSheetName().length()>0 ? arg.obtainInput().from().getSheetName():funcDesc.getSheet();
        	updateInputRange(wbEvaluator,sheetName,arg,(ValueEval) args[i], ii, jj);
        }
		if(LOG.isTraceEnabled()){
			String asked = " Function "+funcName+" invoked with parameters:";
			for(int i=0;i<args.length;i++){
				asked = asked+SMHelper.valueToString(args[i])+"\n";
			}
			LOG.trace(asked);
		}
        ValueEval result = evaluateReturn(funcDesc.getReturnType(),funcDesc.isReturnCollection(), funcDesc.getSheet(),funcDesc.obtainReturnSpace(), ec, pool);
		if(LOG.isTraceEnabled()){
			String returned = " Function "+funcName+ " return value:";
			returned = returned+SMHelper.valueToString(result);
			LOG.trace(returned);
		}
        return result;
		
	}
	private void updateInputRange(WorkbookEvaluator wbEvaluator,String sheetName,FunctionArgument farg, ValueEval arg,int ii, int jj){
		if(farg.obtainInput() instanceof Cell){
			ValueEval value = arg;
			if(arg instanceof ArrayEval){
				value = ((ArrayEval)arg).getValue(ii,jj);
			}
			wbEvaluator.updateCell(sheetName,farg.obtainInput().from().getRowIndex(), farg.obtainInput().from().getColumnIndex(),
                value);
		}
		else { // Range
			Range range = (Range)farg.obtainInput();
			for(int i=0;i<range.getHeight();i++){
				for(int j=0;j<range.getWidth();j++){
					ValueEval value = arg;
					if(arg instanceof ArrayEval){
						value = ((ArrayEval)arg).getValue(ii*range.getHeight()+i,jj*range.getWidth()+j);
					}
					int curColIndex = range.from().getColumnIndex()+j;
					int curRowIndex = range.from().getRowIndex()+i;
					wbEvaluator.updateCell(sheetName,curRowIndex, curColIndex, value);
				}
			}
		}
	}
	private ValueEval evaluateReturn(Type returnType,boolean isReturnCollection,String funcSheetName, ExcelSpace returnSpace,OperationEvaluationContext ec, DataPool pool){
		if(returnType==null){
			if(returnSpace instanceof Cell){  // Cell
				return evaluateCell(funcSheetName,(Cell)returnSpace, ec, pool);
			}
			else if(returnSpace instanceof com.exigen.le.smodel.Range){  // Range
				ValueEval[][] result = new ValueEval[returnSpace.getHeight()][returnSpace.getWidth()];
				for(int i=0;i<returnSpace.getHeight();i++){
					for(int j=0;j<returnSpace.getWidth();j++){
						Cell cell = new com.exigen.le.smodel.Cell();
						cell.from().setRow(returnSpace.from().getRow()+i);
						cell.from().setColumnIndex(returnSpace.from().getColumnIndex()+j);
						ValueEval value = evaluateCell(funcSheetName,cell, ec, pool);
						if(value instanceof ArrayEval){ // For Array we take only top-left value
							result[i][j]=((ArrayEval)value).getValue(0, 0);
						}
						else
							result[i][j]=value;
					}
				}
				return new ArrayEval(result);
			}
			else {
				return null;
			}
		}
		else{
			if(returnSpace instanceof Cell){
				return evaluateCell(funcSheetName,(Cell)returnSpace, ec, pool);
			}
			else {
				ServiceModel sm = ThreadEvaluationContext.getServiceModel();
				String sheetName = ((Range)returnSpace).from().getSheetName();
				if(sheetName.length() <=0){
					sheetName = funcSheetName;
				}
				Type calculatedType = returnType;
				if(isReturnCollection){
					calculatedType = new Type("",true);
					MappedProperty child = new MappedProperty();
					child.setName(returnType.getName());
					child.setCollection(true);
					child.setType(returnType);
					List<MappedProperty> childs = new ArrayList<MappedProperty>();
					childs.add(child);
					calculatedType.setChilds(childs);
				}
				TypifiedRange typifiedRange = new TypifiedRange(calculatedType,sheetName,(Range)returnSpace,sm,ec);
				StringEval objectID = new StringEval(pool.add(typifiedRange.getObject()));
				return LiveExcelEvaluator.createEvalForObject(objectID,ec.getWorkbookEvaluator(), pool);
			}
		}
	}
	private ValueEval evaluateCell(String funcSheetName,Cell cell,OperationEvaluationContext ec, DataPool pool){
    	String sheetName = cell.getSheetName().length()>0 ? cell.getSheetName():funcSheetName;
    	if(sheetName == null){
    		String msg = "Sheet name is not defined neither for function "+funcName+", neither return cell "+cell.toString();
    		LOG.error(msg);
    		throw new RuntimeException(msg);
    	}
        EvaluationCell cellToEvaluate = ec.getWorkbook().getSheet(ec.getWorkbook().getSheetIndex(sheetName)).getCell(cell.getRowIndex(),cell.getColumnIndex());
        if(cellToEvaluate == null){
    		String msg = "Wrong return cell "+cell.toString()+" on sheet "+sheetName+" for function "+funcName;
    		LOG.error(msg);
    		throw new RuntimeException(msg);
        }
        
        return ec.getWorkbookEvaluator().evaluate(cellToEvaluate);
		
	}
	private class Loops {
		int rowCount;
		int colCount;
		Loops(List<FunctionArgument> defined, ValueEval[] received, WorkbookEvaluator wbEvaluator, DataPool pool){
			rowCount=1;
			colCount=1;
			
			for(int i= 0;i<received.length;i++){
				FunctionArgument farg = defined.get(i);
				if(farg.obtainInput() instanceof Range ){
					ValueEval arg = dereferenceArgument(received[i], wbEvaluator, pool);
					if(!(arg instanceof ArrayEval)){
						 	String msg ="Argument with index  "+i+" need to be array ";
				    		LOG.error(msg);
				    		throw new RuntimeException(msg);
					}
					ArrayEval array = (ArrayEval)arg;
					Range range = (Range)farg.obtainInput();
					if(range.getWidth() < array.getWidth()){
						if((array.getWidth()%range.getWidth())!= 0){
					   		String msg ="Argument with index  "+i+" need to have divisible dimensions";
				    		LOG.error(msg);
				    		throw new RuntimeException(msg);
						}
						int curColCount = array.getWidth()/range.getWidth();
						if(curColCount > colCount){
							if(i>0){
						   		String msg ="All arrays arguments need to have same dimension factor, but argument "+i+" has "+curColCount+",while previous "+ colCount;
					    		LOG.error(msg);
					    		throw new RuntimeException(msg);
							}
				    		colCount = curColCount; 
						}
					}	
					if(range.getHeight() < array.getHeight()){
						if((array.getHeight()%range.getHeight())!= 0){
					   		String msg ="Argument with index  "+i+" need to have divisible dimensions";
				    		LOG.error(msg);
				    		throw new RuntimeException(msg);
						}
						int curRowCount = array.getHeight()/range.getHeight();
						if(curRowCount > rowCount){
							if(i>0){
						   		String msg ="All arrays arguments need to have same dimension factor, but argument "+i+" has "+curRowCount+",while previous "+ rowCount;
					    		LOG.error(msg);
					    		throw new RuntimeException(msg);
							}
				    		rowCount = curRowCount; 
						}
					}
				}
			}
		}
	}
	private static ValueEval dereferenceArgument(ValueEval arg, WorkbookEvaluator evaluator, DataPool pool){
		ValueEval result = arg;
		if(arg instanceof StringEval){
			if(pool.isPoolObject(((StringEval)arg).getStringValue())){
				Object ob = pool.get(((StringEval)arg).getStringValue());
				result = LiveExcelEvaluator.createEvalForObject(ob, evaluator, pool);
				}
			
		}
		return result;
	}
}
