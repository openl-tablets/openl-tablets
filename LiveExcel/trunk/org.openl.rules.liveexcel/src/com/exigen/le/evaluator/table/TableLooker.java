/**
 * 
 */
package com.exigen.le.evaluator.table;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.poi.hssf.record.formula.eval.ErrorEval;
import org.apache.poi.hssf.record.formula.eval.RefEval;
import org.apache.poi.hssf.record.formula.eval.StringEval;
import org.apache.poi.hssf.record.formula.eval.ValueEval;
import org.apache.poi.hssf.record.formula.functions.FreeRefFunction;
import org.apache.poi.ss.formula.CollaboratingWorkbooksEnvironment;
import org.apache.poi.ss.formula.EvaluationCell;
import org.apache.poi.ss.formula.OperationEvaluationContext;
import org.apache.poi.ss.formula.WorkbookEvaluator;
import org.apache.poi.ss.formula.CollaboratingWorkbooksEnvironment.WorkbookNotFoundException;
import org.apache.poi.ss.formula.eval.forked.ForkedEvaluationWorkbook;
import org.apache.poi.ss.formula.eval.forked.ForkedEvaluator;
import org.apache.poi.ss.usermodel.Workbook;

import com.exigen.le.evaluator.ThreadEvaluationContext;
import com.exigen.le.evaluator.table.LETableFactory.TableElement;
import com.exigen.le.project.ProjectElement;
import com.exigen.le.project.ProjectLoader;
import com.exigen.le.smodel.ExcelSpace;
import com.exigen.le.smodel.SMHelper;
import com.exigen.le.smodel.TableDesc;
import com.exigen.le.usermodel.LiveExcelWorkbook;

/**
 * Lookup table executor
 * @author vabramovs
 *
 */
public class TableLooker implements FreeRefFunction {
	private static final Log LOG = LogFactory.getLog(TableLooker.class);
	
	private TableDesc tableDesc;
	
	private String tablesFileName;  // This is Repository item name (file name with extension) which contains the date of all tables in projects(it's real version)
//	private  TableElement te;
	public static final String REF_PREFIX = "__LE_REF__";
	
	// This code creates table implementation te in constructor (via LETableFactory call from ProjectCache), that may cause to unneeded memory waste, if it will be no real table invoke
	// To avoid such behaviour shift this code into evaluate method 
	public TableLooker(TableDesc tableDesc){
		this.tableDesc=tableDesc;
		
		tablesFileName = getTablesFileName();
		
		

		
	} 
	private String getTablesFileName(){
        for(ProjectElement element :ProjectLoader.retrieveElementList(ThreadEvaluationContext.getProject())){
            if(element.getType()!=null && element.getType().equals(ProjectElement.ElementType.TABLE)){
                return element.getElementFileName();
            }
        }
	    //TODO find tables else next:
		String msg = "Project did not contains tables data, but has reference to table "+tableDesc.getName();
		LOG.error(msg);
		throw new RuntimeException(msg);
	}
	private TableElement getOrCreateTableElement(String tablesFileName){
		return (TableElement)ProjectLoader.getFullElement(ThreadEvaluationContext.getProject(),tablesFileName, ProjectElement.ElementType.TABLE).getElement();
	}
	/* (non-Javadoc)
	 * @see org.apache.poi.hssf.record.formula.functions.FreeRefFunction#evaluate(org.apache.poi.hssf.record.formula.eval.ValueEval[], org.apache.poi.ss.formula.OperationEvaluationContext)
	 */
	public ValueEval evaluate(ValueEval[] args, OperationEvaluationContext ec) {
		ValueEval answer;
		if(tableDesc.getBasicParametersCount() != args.length){
				String msg="Table "+tableDesc.getName()+" invoke with wrong arguments count(need "+tableDesc.getBasicParametersCount()+",but were "+args.length;
				LOG.error(msg);
	           return  ErrorEval.VALUE_INVALID;
	           }
		// Resolve all reference before calculating
	    for(int i=0;i<args.length;i++){
	    	if(args[i] instanceof RefEval){
		    	args[i]= ((RefEval)args[i]).getInnerValueEval();
	    	}
	    }
		if(LOG.isTraceEnabled()){
			String asked = " Table "+tableDesc.getName()+" asked with parameters:";
			for(int i=0;i<args.length;i++){
				asked = asked+SMHelper.valueToString(args[i])+"\n";
				
			}
			LOG.trace(asked);
		}

	    // Add additional
	    List<String> addNames = tableDesc.getAdditionalParameters();
	    List<ValueEval> addValues = new ArrayList<ValueEval>();
	    for(String addName:addNames){
	    	String value = ThreadEvaluationContext.getEnvProperties().get(addName);
	    	if(value != null){
	    		addValues.add(new StringEval(value));
	    	}
	    	else{
	    		addValues.add(new StringEval(""));
	    	}
	    }
	    // New arguments;
	    ValueEval[] params = new ValueEval[args.length+addValues.size()];
	    System.arraycopy(args, 0, params, 0, args.length);
	    for(int i=args.length;i<params.length;i++){
	    	params[i]=addValues.get(i-args.length);
	   	}
	    
	    
	    // Invoke table implementation
	   
		try {
			TableElement te = getOrCreateTableElement(tablesFileName);
			ValueEval result = te.calculate(tableDesc.getName(), params);
			if (result instanceof StringEval  &&
					((StringEval) result).getStringValue().startsWith(REF_PREFIX)
			){
				// it's a reference - data should be taken from excel sheet
				String refString =((StringEval) result).getStringValue().substring(REF_PREFIX.length());
				answer =  evaluateRef(refString, ec);
			} else {
				answer = result; 
			}
		} catch (Exception e) {
			String msg = " Error looking table "+tableDesc.getName()+ " for parameters:\n";
			for(int i=0;i<params.length;i++){
				msg=msg+SMHelper.valueToString(params[i])+"\n";
			}
			LOG.error(msg,e);
			throw new RuntimeException(msg,e);
		}
		if(LOG.isTraceEnabled()){
			String returned = " Table "+tableDesc.getName()+" returns value:";
			returned = returned+SMHelper.valueToString(answer);
			LOG.trace(returned);
		}
		return answer;
	}
 // Resolve reference if it is used as table value	
 private ValueEval evaluateRef(String refString, OperationEvaluationContext ec ){
		// refString is in format [<excel>]<sheet>!<cell>
		ExcelSpace ref = ExcelSpace.Factory.create(refString);
		String sheetName = ref.from().getSheetName();
		String workbookName = ref.from().getWorkbookName();
		if( sheetName.isEmpty()){
			sheetName = ec.getRefEvaluatorForCurrentSheet().getSheetName();
		}
		WorkbookEvaluator wbe =  ec.getWorkbookEvaluator();
		ForkedEvaluationWorkbook wb = (ForkedEvaluationWorkbook)wbe.getEvaluationWorkbook();
		if(workbookName.length()>0){
				Workbook refBook = ProjectLoader.getWorkbook(ThreadEvaluationContext.getProject(), workbookName);
				if(! refBook.equals(ec.getWorkbook().getWorkbook()) ){
					// Other workbook
					workbookName = wb.translateExternalWorkbookRef(workbookName);
					try { // May be this workbook is registered
						wbe  = 	wbe.getorCreateOtherWorkbookEvaluator(workbookName);
						wb = (ForkedEvaluationWorkbook)wbe.getEvaluationWorkbook();
					} catch (WorkbookNotFoundException e) { // not registered yet - create and register
						CollaboratingWorkbooksEnvironment env = wbe.get_collaboratingWorkbookEnvironment();
						String qualifiedName = wbe.buildQualifiedName(workbookName);
						ForkedEvaluator fe = ForkedEvaluator.create(refBook,  null,((LiveExcelWorkbook)refBook).getUDFFinder());
						wbe = fe.getWorkbookEvaluator();
						env.addWorkbookEvaluator(qualifiedName, wbe);
					}
				}
		}
		wb = (ForkedEvaluationWorkbook)wbe.getEvaluationWorkbook();
		EvaluationCell srcCell = wb.getEvaluationCell(sheetName, ref.from().getRowIndex(), ref.from().getColumnIndex());
		return wbe.evaluate(srcCell);
 	}
}
