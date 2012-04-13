package org.openl.rules.liveexcel.formula;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

/**
 * Looks for all declarations of LiveExcel functions in all sheets in file
 * 
 * @author DLiauchuk
 */
public class DeclaredFunctionSearcher {

    private static final Log log = LogFactory.getLog(DeclaredFunctionSearcher.class);

    private Workbook workbook;

    public DeclaredFunctionSearcher(Workbook workbook) {
        this.workbook = workbook;
        LiveExcelFunctionsPack.instance().createUDFFinderLE(workbook);

        // TODO: remove after integration with liveexcel plugin
        LiveExcelFunctionsPack.registerFunctionNameInWorkbook(workbook, LiveExcelFunctionsPack.OL_DECLARATION_FUNCTION);
        LiveExcelFunctionsPack.registerFunctionNameInWorkbook(workbook,
                LiveExcelFunctionsPack.OL_DECLARATION_LOOKUP_TABLE);
    }

    public void findFunctions() {
        FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator(
                LiveExcelFunctionsPack.instance());
        for (int num = 0; num < workbook.getNumberOfSheets(); num++) {
            Sheet sheet = workbook.getSheetAt(num);
            for (Row row : sheet) {
                for (Cell cell : row) {
                    if (isTypeFormula(cell)) {
                        DataFormatter dataFormatter = new DataFormatter();
                        String formattedValue = dataFormatter.formatCellValue(cell);
                        if (LiveExcelFunctionsPack.isLiveExcelGlobalFunction(formattedValue)) {
                            evaluator.evaluate(cell);
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
