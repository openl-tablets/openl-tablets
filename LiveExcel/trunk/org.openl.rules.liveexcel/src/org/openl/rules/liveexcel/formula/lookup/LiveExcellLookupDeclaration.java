package org.openl.rules.liveexcel.formula.lookup;

import java.util.ArrayList;
import java.util.List;

import org.apache.poi.hssf.record.formula.eval.AreaEval;
import org.apache.poi.hssf.record.formula.eval.Eval;
import org.apache.poi.hssf.record.formula.eval.StringEval;
import org.apache.poi.hssf.record.formula.eval.StringValueEval;
import org.apache.poi.hssf.record.formula.eval.ValueEval;
import org.apache.poi.hssf.record.formula.functions.FreeRefFunction;
import org.apache.poi.ss.formula.EvaluationWorkbook;
import org.openl.rules.liveexcel.LiveExcelException;
import org.openl.rules.liveexcel.formula.FunctionParam;

/**
 * Class for registration new declared user lookups(declared by
 * "OL_DECLARE_TABLE").
 * 
 * @author PUdalau
 */
public class LiveExcellLookupDeclaration implements FreeRefFunction {

    public ValueEval evaluate(Eval[] args, EvaluationWorkbook workbook, int srcCellSheet, int srcCellRow, int srcCellCol) {
        String functionName = ((StringValueEval) args[0]).getStringValue();
        AreaEval lookupArea = (AreaEval) args[args.length - 1];
        FreeRefFunction lookup = workbook.getWorkbook().getUserDefinedFunction(functionName);
        if (lookup == null) {
            lookup = createLookup(lookupArea);
        } else {
            if (lookup instanceof LiveExcelLookup) {
                lookup = expandLookup(lookupArea, ((LiveExcelLookup) lookup).getLookupData(), functionName);
            } else {
                throw new LiveExcelException("Function \"" + functionName + "\" has been already declared");
            }
        }
        ((LiveExcelLookup) lookup).setDeclFuncName(functionName);
        initParameters((LiveExcelLookup) lookup, args);
        workbook.getWorkbook().registerUserDefinedFunction(functionName, lookup);
        return new StringEval(functionName);
    }

    private void initParameters(LiveExcelLookup lookup, Eval[] args) {
        if (args.length == 3) {
            lookup.setReturnCell(new FunctionParam(((StringEval) args[1]).getStringValue(), null));
        } else {
            lookup.setReturnCell(new FunctionParam("", null));
        }
        List<FunctionParam> params = new ArrayList<FunctionParam>();
        for (int i = 0; i < lookup.getLookupData().getWidth() - 1; i++) {
            // TODO: check param type also
            params.add(new FunctionParam("", null));
        }
        lookup.setParameters(params);
    }

    private LiveExcelLookup createLookup(AreaEval area) {
        LiveExcelLookup lookup = new LiveExcelLookup(new LookupGridParser(area).createLookupData());
        return lookup;
    }

    private LiveExcelLookup expandLookup(AreaEval area, Grid previousGrid, String functionName) {
        Grid expansionGrid = new LookupGridParser(area).createLookupData();
        if (previousGrid.getWidth() != expansionGrid.getWidth()) {
            throw new LiveExcelException("Function \"" + functionName
                    + "\" has several lookup data areas with different dimensions");
        }
        return new LiveExcelLookup(new CompositeGrid(previousGrid, expansionGrid));
    }

}
