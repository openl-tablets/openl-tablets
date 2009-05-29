package org.openl.rules.liveexcel.formula;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.poi.hssf.usermodel.HSSFDataFormatter;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.util.CellReference;

public class DeclareSearcher {
    final String OL_DECLARATION_FUNCTION = "ol_declare_function";
    HSSFWorkbook workbook;
    List<DeclaredFunction> functionsToParse = new ArrayList<DeclaredFunction>();

    public DeclareSearcher(String fileName) {
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
                        if (formattedValue.startsWith(OL_DECLARATION_FUNCTION)) {                            
                            DeclaredFunction declFunc = new DeclaredFunction(sheet,cellRef.formatAsString(),formattedValue);
                            functionsToParse.add(declFunc);                      
                        }
                        
                    }                      
                }
            }
        }
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
    
    /*private void determineCellValue(Cell cell) {
        switch (cell.getCellType()) {
            case Cell.CELL_TYPE_BLANK:
                System.out.println("TYPE IS BLANK");
                break;
            case Cell.CELL_TYPE_BOOLEAN:
                System.out.println("TYPE IS BOOLEAN");
                break;
            case Cell.CELL_TYPE_FORMULA:
                System.out.println("TYPE IS FORMULA");
                break;
            case Cell.CELL_TYPE_NUMERIC:
                System.out.println("TYPE IS NUMERIC");
                break;
            case Cell.CELL_TYPE_STRING:
                System.out.println("TYPE IS STRING");
                break;
            case Cell.CELL_TYPE_ERROR:
                System.out.println("TYPE IS ERROR");
                break;
            default:
                System.out.println("TYPE IS UNKNOWN");
                break;
        }
        System.out.println("------------\n");
    }*/

}
