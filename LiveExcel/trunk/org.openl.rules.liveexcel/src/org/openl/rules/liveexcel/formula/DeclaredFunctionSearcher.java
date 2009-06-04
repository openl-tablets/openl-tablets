package org.openl.rules.liveexcel.formula;

import org.apache.log4j.Logger;

import org.apache.poi.hssf.record.formula.toolpack.MainToolPacksHandler;
import org.apache.poi.hssf.usermodel.HSSFDataFormatter;
import org.apache.poi.hssf.usermodel.HSSFFormulaEvaluator;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFFormulaEvaluator;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 * Looks for all declarations of LiveExcel functions in all sheets in file
 * 
 * @author DLiauchuk
 */
public class DeclaredFunctionSearcher {

    public final static String OL_DECLARATION_FUNCTION = "OL_DECLARE_FUNCTION";
    static Logger log4j = Logger.getLogger("org.openl.rules.liveexcel.formula");

    private Workbook workbook;

    public DeclaredFunctionSearcher(Workbook workbook) {
        this.workbook = workbook;

        // TODO: remove
        workbook.registerUserDefinedFunction(OL_DECLARATION_FUNCTION, null);

        MainToolPacksHandler packHandler = MainToolPacksHandler.instance();
		if (!packHandler.containsFunction(OL_DECLARATION_FUNCTION)) {
        	LiveExcelFunctionsPack liveExcelPack = new LiveExcelFunctionsPack();
            liveExcelPack.addFunction(OL_DECLARATION_FUNCTION, new LiveExcellFunctionDeclaration());
            packHandler.addToolPack(liveExcelPack);
        }
    }

    public void findFunctions() {
        for (int num = 0; num < workbook.getNumberOfSheets(); num++) {
            Sheet sheet = workbook.getSheetAt(num);
            for (Row row : sheet) {
                for (Cell cell : row) {
                    if (isTypeFormula(cell)) {
                        HSSFDataFormatter dFormatter = new HSSFDataFormatter();
                        String formattedValue = dFormatter.formatCellValue(cell);
                        if (formattedValue.toUpperCase().startsWith(OL_DECLARATION_FUNCTION)) {
                            if (workbook instanceof HSSFWorkbook) {
                                new HSSFFormulaEvaluator((HSSFWorkbook) workbook).evaluate(cell);
                            } else if (workbook instanceof XSSFWorkbook) {
                                new XSSFFormulaEvaluator((XSSFWorkbook) workbook).evaluate(cell);
                            }
                        }
                    }
                }
            }
        }
    }

    private boolean isTypeFormula(Cell cell) {
        boolean result = false;
        if (cell.getCellType() == Cell.CELL_TYPE_FORMULA) {
            result = true;
        }
        return result;
    }
}
