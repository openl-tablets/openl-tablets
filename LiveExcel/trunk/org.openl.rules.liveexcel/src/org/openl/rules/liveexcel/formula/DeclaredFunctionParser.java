package org.openl.rules.liveexcel.formula;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import org.apache.poi.hssf.record.formula.eval.Eval;
import org.apache.poi.hssf.record.formula.eval.RefEval;
import org.apache.poi.hssf.record.formula.eval.StringEval;

/**
 * Parses defined ol_declare_function functions
 * 
 * @author DLiauchuk
 */
public class DeclaredFunctionParser {

    final Logger LOGGER = Logger.getLogger(DeclaredFunctionParser.class);

    private Eval[] arguments;

    String formulaString;
    private int pointer = 0;
    private ParsedDeclaredFunction parsDeclFunc = new ParsedDeclaredFunction();
    private List<FunctionParam> listParams = new ArrayList<FunctionParam>();

    private DeclaredFunctionParser(Eval[] arguments) {
        this.arguments = arguments;
    }

    public static ParsedDeclaredFunction parseFunction(Eval[] arguments) {
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
                checkName();
                checkReturn();
                checkParameters();
                parsDeclFunc.setParameters(listParams);
            } else {
                throw expected("function definition must contain min name and output cell");
            }
        } catch (MissingRequiredParametersException e) {
            e.printStackTrace();
        }

    }

    private void checkName() {
        pointer = 0;
        if (arguments[pointer] instanceof StringEval) {
            parsDeclFunc.setDeclFuncName(((StringEval) arguments[pointer]).getStringValue());
            pointer++;
        } else {
            throw expected("first argument must be a declared function name");
        }
    }

    private void checkReturn() {
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

    private void checkParameters() {
        if (pointer + 1 < arguments.length) {
            listParams.add(extractNextParam());
            checkParameters();
        }
    }

    /** Report What Was Expected */
    private RuntimeException expected(String s) {
        String msg;
        msg = "The specified formula '" + formulaString + "' was expected: " + s;

        return new MissingRequiredParametersException(msg);
    }
}
