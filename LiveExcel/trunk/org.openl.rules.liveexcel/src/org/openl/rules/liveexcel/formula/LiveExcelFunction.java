package org.openl.rules.liveexcel.formula;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.poi.hssf.record.formula.eval.ErrorEval;
import org.apache.poi.hssf.record.formula.eval.RefEval;
import org.apache.poi.hssf.record.formula.eval.ValueEval;
import org.apache.poi.hssf.record.formula.functions.FreeRefFunction;
import org.apache.poi.ss.formula.OperationEvaluationContext;

/**
 * Common class for LiveExcel functions.
 * 
 * @author PUdalau
 */
public abstract class LiveExcelFunction implements FreeRefFunction {
    private static final Log log = LogFactory.getLog(LiveExcelFunction.class);
    protected String declFuncName;

    /**
     * @return Name of function.
     */
    public String getDeclFuncName() {
        return declFuncName;
    }

    /**
     * Sets name of function.
     * 
     * @param declFuncName Name of function.
     */
    public void setDeclFuncName(String declFuncName) {
        this.declFuncName = declFuncName;
    }

    public ValueEval evaluate(ValueEval[] args, OperationEvaluationContext ec) {
        ValueEval[] processedArgs = prepareArguments(args);
        log.info("Execution of function [" + declFuncName + "]started. Arguments: " + printArguments(processedArgs));
        ValueEval result = execute(processedArgs, ec);
        if (result instanceof ErrorEval) {
            if (result == ErrorEval.VALUE_INVALID) {
                log.error("Wrong arguments for function [" + declFuncName + "]");
            } else if (result == ErrorEval.NA) {
                log.warn("No output available from function [" + declFuncName + "]");
            } else {
                log.error("Error in function [" + declFuncName + "]" + "Error message :"
                        + ErrorEval.getText(((ErrorEval) result).getErrorCode()));
            }
        } else {
            log.info("Result of function [" + declFuncName + "] : " + result);
        }
        return result;
    }

    public ValueEval[] prepareArguments(ValueEval[] args) {
        ValueEval[] result = args.clone();
        for (int i = 0; i < args.length; i++) {
            if (args[i] instanceof RefEval) {
                result[i] = ((RefEval) args[i]).getInnerValueEval();
            } else {
                result[i] = args[i];
            }
        }
        return result;
    }

    private static String printArguments(ValueEval[] args) {
        StringBuffer buffer = new StringBuffer("[");
        for (ValueEval argument : args) {
            buffer.append(argument);
            buffer.append(';');
        }
        buffer.append(']');
        return buffer.toString();
    }

    protected abstract ValueEval execute(ValueEval[] args, OperationEvaluationContext ec);
}
