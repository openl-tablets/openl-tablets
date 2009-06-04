package org.openl.rules.liveexcel.formula;

import org.apache.log4j.Logger;

import org.apache.poi.hssf.record.formula.toolpack.MainToolPacksHandler;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

/**
 * Looks for all declarations of LiveExcel functions in all sheets in file
 * 
 * @author DLiauchuk
 */
public class DeclaredFunctionSearcher {

    public static final String OL_DECLARATION_FUNCTION = "OL_DECLARE_FUNCTION";

    private static final Logger log = Logger.getLogger(DeclaredFunctionSearcher.class);

    private Workbook workbook;

    public DeclaredFunctionSearcher(Workbook workbook) {
        this.workbook = workbook;

        // TODO: remove after integration with liveexcel plugin
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
                        DataFormatter dataFormatter = new DataFormatter();
                        String formattedValue = dataFormatter.formatCellValue(cell);
                        if (formattedValue.toUpperCase().startsWith(OL_DECLARATION_FUNCTION)) {
                            workbook.getCreationHelper().createFormulaEvaluator().evaluate(cell);
                        }
                    }
                }
            }
        }
    }

    private boolean isTypeFormula(Cell cell) {
        return cell.getCellType() == Cell.CELL_TYPE_FORMULA;
    }
}
