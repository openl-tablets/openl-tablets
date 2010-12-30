package org.openl.rules.method;

import org.openl.exception.OpenlNotCheckedException;
import org.openl.rules.calc.Spreadsheet;
import org.openl.rules.calc.trace.SpreadsheetTraceObject;
import org.openl.rules.cmatch.ColumnMatch;
import org.openl.rules.cmatch.algorithm.ColumnMatchTraceObject;
import org.openl.rules.dt.DecisionTable;
import org.openl.rules.dt.trace.DecisionTableTraceObject;
import org.openl.rules.method.table.MethodTableTraceObject;
import org.openl.rules.method.table.TableMethod;
import org.openl.rules.table.ATableTracerNode;
import org.openl.rules.tbasic.Algorithm;
import org.openl.rules.tbasic.runtime.debug.TBasicAlgorithmTraceObject;
import org.openl.types.IOpenMethod;

public class TracedObjectFactory {
    
    public static ATableTracerNode getTracedObject(IOpenMethod method, Object[] params) {
        if (method instanceof Algorithm) {
            return new TBasicAlgorithmTraceObject((Algorithm)method, params);
        } else if (method instanceof ColumnMatch) {
            return new ColumnMatchTraceObject((ColumnMatch)method, params);
        } else if (method instanceof DecisionTable) {
            return new DecisionTableTraceObject((DecisionTable)method, params);
        } else if (method instanceof TableMethod) {
            return new MethodTableTraceObject((TableMethod)method, params);
        } else if (method instanceof Spreadsheet) {
            return new SpreadsheetTraceObject((Spreadsheet)method, params);
        } 
        throw new OpenlNotCheckedException(String.format("Can`t create traced object for %s.", method.getName()));
    }

}
