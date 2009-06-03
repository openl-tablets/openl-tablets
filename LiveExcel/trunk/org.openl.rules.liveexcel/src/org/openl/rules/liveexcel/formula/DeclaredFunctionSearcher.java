package org.openl.rules.liveexcel.formula;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import org.apache.poi.hssf.model.HSSFFormulaParser;
import org.apache.poi.hssf.record.formula.Ptg;
import org.apache.poi.hssf.usermodel.HSSFDataFormatter;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.util.CellReference;


/**
 * Looks for all declarations of LiveExcel functions in all sheets in file  
 * 
 * @author DLiauchuk
 */
public class DeclaredFunctionSearcher {
    
    static Logger log4j = Logger.getLogger("org.openl.rules.liveexcel.formula");    
    
    private HSSFWorkbook workbook;
    private List<DeclaredFunction> functionsToParse = new ArrayList<DeclaredFunction>();
    
   
    public DeclaredFunctionSearcher(String fileName) {
        InputStream is = null;
        try {
            is = new FileInputStream(fileName);
            POIFSFileSystem fs = new POIFSFileSystem(is);
            workbook = new HSSFWorkbook(fs);            
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (is != null)
                    is.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    
    public void findFunctions(){
        for(int num=0;num<workbook.getNumberOfSheets();num++){
            HSSFSheet sheet = workbook.getSheetAt(num);            
            for (Row row : sheet) {
                for (Cell cell : row) {
                    CellReference cellRef = new CellReference(row.getRowNum(), cell.getColumnIndex());    
                    
                    if(isTypeFormula(cell)) {
                        HSSFDataFormatter dFormatter = new HSSFDataFormatter();
                        String formattedValue = dFormatter.formatCellValue(cell);                        
                        if (formattedValue.startsWith(DeclaredFunctionParser.OL_DECLARATION_FUNCTION)) {                            
                            DeclaredFunction declFunc = new DeclaredFunction(sheet,cellRef.formatAsString(),formattedValue);
                            functionsToParse.add(declFunc);
                        }
                        
                    }                      
                }
            }
        }
       // List<ParsedDeclaredFunction> parsedFunctions = parse(functionsToParse);        
    }
    
    public List<ParsedDeclaredFunction> parse(List <DeclaredFunction> listDeclFunc) {
        List<ParsedDeclaredFunction> parsedFunctions = new ArrayList<ParsedDeclaredFunction>(); 
        for(DeclaredFunction decFun : listDeclFunc) {
            ParsedDeclaredFunction parsFunc = DeclaredFunctionParser.parseFunction(decFun.getFunctionText(),workbook);            
            parsedFunctions.add(parsFunc);
        }
        return parsedFunctions;
    }
    
    public void setFunctionsToParse(List<DeclaredFunction> functionsToParse) {
        this.functionsToParse = functionsToParse;
    }
    
    public List<DeclaredFunction> getFunctionsToParse() {
        return functionsToParse;
    } 
    
    private boolean isTypeFormula(Cell cell) {
        boolean result = false;
        if(cell.getCellType()==Cell.CELL_TYPE_FORMULA){
            result = true;
        }
        return result;
    }
}
