package org.openl.rules.liveexcel.formula.lookup;

import org.apache.poi.hssf.record.formula.eval.AreaEval;
import org.apache.poi.hssf.record.formula.eval.StringEval;
import org.apache.poi.hssf.record.formula.eval.StringValueEval;
import org.apache.poi.hssf.record.formula.eval.ValueEval;
import org.apache.poi.hssf.record.formula.functions.FreeRefFunction;
import org.apache.poi.ss.formula.EvaluationWorkbook;
import org.apache.poi.ss.formula.OperationEvaluationContext;
import org.openl.rules.liveexcel.formula.LiveExcelFunctionsPack;

/**
 * Class for registration new declared user lookups(declared by
 * "OL_DECLARE_TABLE").
 * 
 * @author PUdalau
 */
public class LiveExcellLookupDeclaration implements FreeRefFunction {

    public ValueEval evaluate(ValueEval[] args, OperationEvaluationContext ec) {
        EvaluationWorkbook evaluationWorkbook = ec.getWorkbook();
        String functionName = ((StringValueEval) args[0]).getStringValue();
        AreaEval lookupArea = (AreaEval) args[args.length - 1];
        LiveExcelLookup lookup = createLookup(lookupArea);
        lookup.setDeclFuncName(functionName);
        if (args.length == 3) {
            new LookupTypeResolver(lookup).initParameters(((StringEval) args[1]).getStringValue());
        } else {
            new LookupTypeResolver(lookup).initParameters("");
        }
        LiveExcelFunctionsPack.instance().addUDF(evaluationWorkbook.getWorkbook(), lookup.getDeclFuncName(), lookup);
        return new StringEval(functionName);
    }

    private LiveExcelLookup createLookup(AreaEval area) {
        LiveExcelLookup lookup = new LiveExcelLookup(new LookupGridParser(area).createLookupData());
        return lookup;
    }

//    private LiveExcelLookup expandLookup(AreaEval area, Grid previousGrid, String functionName) {
//        Grid expansionGrid = new LookupGridParser(area).createLookupData();
//        if (previousGrid.getWidth() != expansionGrid.getWidth()) {
//            throw new LiveExcelException("Function \"" + functionName
//                    + "\" has several lookup data areas with different dimensions");
//        }
//        return new LiveExcelLookup(new CompositeGrid(previousGrid, expansionGrid));
//    }

}
