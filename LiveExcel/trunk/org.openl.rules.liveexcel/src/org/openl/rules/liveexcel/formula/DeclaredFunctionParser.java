package org.openl.rules.liveexcel.formula;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import org.apache.poi.hssf.record.formula.Ptg;
import org.apache.poi.hssf.record.formula.RefPtg;
import org.apache.poi.hssf.usermodel.HSSFEvaluationWorkbook;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.formula.FormulaParser;
import org.apache.poi.hssf.record.formula.StringPtg;

/**
 * Parses defined ol_declare_function functions
 * 
 * @author DLiauchuk
 */
public class DeclaredFunctionParser {    
    
    final static String OL_DECLARATION_FUNCTION = "ol_declare_function";
    final Logger LOGGER = Logger.getLogger(DeclaredFunctionParser.class);    
    
    HSSFWorkbook workbook;
    String formulaString;
    int pointer = 0;
    Ptg[] allPtgs;
    ParsedDeclaredFunction parsDeclFunc = new ParsedDeclaredFunction();
    List<FunctionParam> listParams = new ArrayList<FunctionParam>();   
    
    private DeclaredFunctionParser(String formulaString, HSSFWorkbook workbook) {   
        this.formulaString = formulaString;
        this.workbook = workbook;
    }
    
    public static ParsedDeclaredFunction parseFunction(String functionDeclaration, HSSFWorkbook workbook) {       
        DeclaredFunctionParser parser = new DeclaredFunctionParser(functionDeclaration, workbook);
        parser.parse();
        return parser.getParsedDeclFunc();
    }
    
    private ParsedDeclaredFunction getParsedDeclFunc() {        
        return parsDeclFunc;
    }

    private void parse() {
        UDFAdder adder = new UDFAdder(OL_DECLARATION_FUNCTION, workbook);
        adder.makeUDF();
        HSSFEvaluationWorkbook evalWorkBook = HSSFEvaluationWorkbook.create(workbook);
        allPtgs = FormulaParser.parse(formulaString, evalWorkBook);
        
        try{
            if(allPtgs.length>=3){        
                checkName();
            } else {
                throw expected("function definition must contain min name and output cell");
            }
        } catch (MissingRequiredParametersException e) {
            e.printStackTrace();
        }
        
    }
    
    private void checkName() {
        pointer = 1;
        try{
            if (allPtgs[pointer] instanceof StringPtg) {            
                parsDeclFunc.setDeclFuncName(allPtgs[pointer].toFormulaString().
                        substring(1, allPtgs[pointer].toFormulaString().length()-1));
                pointer++;
                checkDescription();
            } else {
                throw expected("first argument must be a declared function name");
            }
        } catch (MissingRequiredParametersException e) {
            e.printStackTrace();
        }
    }
    
    private void checkDescription() {  
        try{
            if(allPtgs[pointer] instanceof StringPtg&&allPtgs[pointer+1] instanceof RefPtg) {
                parsDeclFunc.setDescription(allPtgs[pointer].toFormulaString().
                        substring(1, allPtgs[pointer].toFormulaString().length()-1));
                parsDeclFunc.setReturnCell((RefPtg)allPtgs[pointer+1]);  
                pointer = pointer+2;
                checkParameters();
            } else { 
                if (allPtgs[pointer] instanceof RefPtg) {
                    parsDeclFunc.setReturnCell((RefPtg)allPtgs[pointer]);
                    pointer++;
                    checkParameters();
                } else {
                    /*if(p[2] is an array) {
                        also we need to check if 2-nd argument may be an array
                        of 2 references, containing description and implementation cell.
                        Such arrays are not still implemented in POI.                              
                    } else {                            
                    }*/
                    throw expected("second argument must be a declared function description," +
                    		"or if its missed it must be output cell");
                }   
            }
        } catch (MissingRequiredParametersException e) {
            e.printStackTrace();
        }
    }
    
    private void checkParameters() {   
        if(pointer+1<allPtgs.length) {
            try{
                if(allPtgs[pointer] instanceof StringPtg&&allPtgs[pointer+1] instanceof RefPtg) {
                    FunctionParam funcParam = new FunctionParam(allPtgs[pointer].toFormulaString().
                               substring(1, allPtgs[pointer].toFormulaString().length()-1), 
                               (RefPtg) allPtgs[pointer+1]);
                    listParams.add(funcParam);
                    pointer = pointer+2;                    
                    checkParameters();                    
                } else {
                    if(allPtgs[pointer] instanceof RefPtg) {
                        FunctionParam funcParam = new FunctionParam("", (RefPtg) allPtgs[pointer]);
                        listParams.add(funcParam);
                        pointer++;
                        checkParameters();
                        } else {
                            /*if(p[2] is an array) {
                            also we need to check if parameters may be an array
                            of 2 references, containing description and parameter cell.
                            Such arrays are not still implemented in POI.                              
                            } else {                            
                            }*/
                            throw expected("not right parameters definition");
                        }
                    }
                } catch (MissingRequiredParametersException e) {
                    e.printStackTrace();
                }
                
        } else {
            parsDeclFunc.setParameters(listParams);
        }
    
    }
    
    /** Report What Was Expected */
    private RuntimeException expected(String s) {
        String msg;
        msg = "The specified formula '" + formulaString
        + "' was expected: "+s;       
       
        return new MissingRequiredParametersException(msg);
    }
}
