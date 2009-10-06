package org.openl.rules.liveexcel.formula;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.poi.hssf.record.formula.eval.RefEval;
import org.apache.poi.hssf.record.formula.eval.StringEval;
import org.apache.poi.hssf.record.formula.eval.ValueEval;
import org.openl.rules.liveexcel.LiveExcelException;

/**
 * Parses defined ol_declare_function functions
 * 
 * @author DLiauchuk
 */
public class DeclaredFunctionParser {

    private static final Log LOG = LogFactory.getLog(DeclaredFunctionParser.class);

    private ValueEval[] arguments;
    private int pointer = 0;
    private ParsedDeclaredFunction parsDeclFunc = new ParsedDeclaredFunction();
    private List<FunctionParam> listParams = new ArrayList<FunctionParam>();
    
    private String formulaString;

    private DeclaredFunctionParser(ValueEval[] arguments) {
        this.arguments = arguments;
    }
    
    /**
     * Parses arguments of ol_declare_functions.
     * 
     * @param evaluationContext EvaluationContext that presents service model.
     * @param functionName Name of function.
     */
    public static ParsedDeclaredFunction parseFunction(ValueEval[] arguments) {
        DeclaredFunctionParser parser = new DeclaredFunctionParser(arguments);
        parser.parse();
        return parser.getParsedDeclFunc();
    }

    private ParsedDeclaredFunction getParsedDeclFunc() {
        return parsDeclFunc;
    }

    private void parse() {
        try {
            if (arguments.length >= 3) {
                setName();
                setReturn();
                setParameters();
            } else {
                throw expected("function definition must contain min name and output cell");
            }
        } catch (MissingRequiredParametersException e) {
        	LOG.error("Function definition error", e);
        }

    }

    private void setName() {
        pointer = 0;
        if (arguments[pointer] instanceof StringEval) {
            parsDeclFunc.setDeclFuncName(((StringEval) arguments[pointer]).getStringValue());
            pointer++;
        } else {
            throw expected("first argument must be a declared function name");
        }
    }

    private void setReturn() {
        parsDeclFunc.setReturnCell(extractNextParam());
    }

    private FunctionParam extractNextParam() {
        FunctionParam funcParam;
        if (arguments[pointer] instanceof StringEval && arguments[pointer + 1] instanceof RefEval) {
            funcParam = new FunctionParam(((StringEval) arguments[pointer]).getStringValue(),
                    (RefEval) arguments[pointer + 1]);
            pointer += 2;
        } else {
            if (arguments[pointer] instanceof RefEval) {
                funcParam = new FunctionParam(null, (RefEval) arguments[pointer]);
                pointer++;
            } else {
                /*
                 * if(p[2] is an array) { also we need to check if 2-nd argument
                 * may be an array of 2 references, containing description and
                 * implementation cell. Such arrays are not still implemented in
                 * POI. } else { }
                 */
                throw expected("second argument must be a declared function description,"
                        + "or if its missed it must be output cell");
            }
        }
        return funcParam;
    }

    private void parseParameters() {
        if (pointer + 1 <= arguments.length) {
            listParams.add(extractNextParam());
            parseParameters();
        }
    }
    
    private void setParameters() {
    	parseParameters();
    	parsDeclFunc.setParameters(listParams);
    }

    /** Report What Was Expected */
    private LiveExcelException expected(String s) {
        String msg = "The specified formula '%1$s' was expected: %2$s";
        return new MissingRequiredParametersException(String.format(msg, formulaString, s));
    }
    
}
