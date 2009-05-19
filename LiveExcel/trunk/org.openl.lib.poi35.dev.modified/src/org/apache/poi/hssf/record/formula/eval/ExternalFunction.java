package org.apache.poi.hssf.record.formula.eval;


import org.apache.poi.hssf.record.formula.atp.AnalysisToolPak;
import org.apache.poi.hssf.record.formula.function.LiveExcelFunction;
import org.apache.poi.hssf.record.formula.function.LiveExcelFunctionsController;
import org.apache.poi.hssf.record.formula.functions.FreeRefFunction;
import org.apache.poi.ss.formula.EvaluationWorkbook;
import org.apache.poi.ss.formula.eval.NotImplementedException;
/**
 * 
 * Common entry point for all user-defined (non-built-in) functions (where 
 * <tt>AbstractFunctionPtg.field_2_fnc_index</tt> == 255)
 * 
 * TODO rename to UserDefinedFunction
 * @author Josh Micich
 */
final class ExternalFunction implements FreeRefFunction {

	public ValueEval evaluate(Eval[] args, EvaluationWorkbook workbook, 
			int srcCellSheet, int srcCellRow,int srcCellCol) {
		
		int nIncomingArgs = args.length;
		if(nIncomingArgs < 1) {
			throw new RuntimeException("function name argument missing");
		}
		
		Eval nameArg = args[0];
		FreeRefFunction targetFunc;
		if (nameArg instanceof NameEval) {
			targetFunc = findInternalUserDefinedFunction(workbook, (NameEval) nameArg);
		} else if (nameArg instanceof NameXEval) {
			targetFunc = findExternalUserDefinedFunction(workbook, (NameXEval) nameArg);
		} else {
			throw new RuntimeException("First argument should be a NameEval, but got ("
					+ nameArg.getClass().getName() + ")");
		}
		int nOutGoingArgs = nIncomingArgs -1;
		Eval[] outGoingArgs = new Eval[nOutGoingArgs];
		System.arraycopy(args, 1, outGoingArgs, 0, nOutGoingArgs);
		return targetFunc.evaluate(outGoingArgs, workbook, srcCellSheet, srcCellRow, srcCellCol);
	}

	private static FreeRefFunction findExternalUserDefinedFunction(EvaluationWorkbook workbook,
			NameXEval n) {
		String functionName = workbook.resolveNameXText(n.getPtg());

		if(false) {
			System.out.println("received call to external user defined function (" + functionName + ")");
		}
		// currently only looking for functions from the 'Analysis TookPak'  e.g. "YEARFRAC" or "ISEVEN"
		// not sure how much this logic would need to change to support other or multiple add-ins.
		FreeRefFunction result = AnalysisToolPak.findFunction(functionName);
		if (result != null) {
			return result;
		}
		throw new NotImplementedException(functionName);
	}

	private static FreeRefFunction findInternalUserDefinedFunction(EvaluationWorkbook workbook, NameEval functionNameEval) {

		String functionName = functionNameEval.getFunctionName();
		if(false) {
			System.out.println("received call to internal user defined function  (" + functionName + ")");
		}
		LiveExcelFunctionsController.instance().findAllLiveExcelFunctions(workbook);
        LiveExcelFunction liveExcelFunction = LiveExcelFunctionsController.instance().getFunction(workbook,
                functionName);
        if (liveExcelFunction != null) {
            return liveExcelFunction;
        }
		
		// TODO find the implementation for the user defined function
		
		throw new NotImplementedException(functionName);
	}
}
