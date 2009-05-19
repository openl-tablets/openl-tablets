package org.apache.poi.hssf.record.formula.function;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.ss.formula.EvaluationWorkbook;
//FIXME: thread-safety
public class LiveExcelFunctionsController {
    private Map<EvaluationWorkbook, Map<String, LiveExcelFunction>> functionsOfWorkbook = new HashMap<EvaluationWorkbook, Map<String, LiveExcelFunction>>();

    private static LiveExcelFunctionsController instance;

    public static LiveExcelFunctionsController instance() {
        if (instance == null) {
            instance = new LiveExcelFunctionsController();
        }
        return instance;
    }

    private LiveExcelFunctionsController() {
    }

    public void declareFunction(EvaluationWorkbook workbook, String functionName, HSSFCell outputCell,
            List<HSSFCell> inputCells) {
        Map<String, LiveExcelFunction> functions = functionsOfWorkbook.get(workbook);
        if (functions == null) {
            // TODO: some error
            return;
        }
        if (functions.containsKey(functionName)) {
            throw new RuntimeException("Function \"" + functionName + "\" has been already declared");
        } else {
            functions.put(functionName, new LiveExcelFunction(functionName, outputCell, inputCells));
        }
    }

    public LiveExcelFunction getFunction(EvaluationWorkbook workbook, String functionName) {
        Map<String, LiveExcelFunction> functions = functionsOfWorkbook.get(workbook);
        if (functions == null) {
            return null;
        }
        return functions.get(functionName);
    }

    public void findAllLiveExcelFunctions(EvaluationWorkbook workbook) {
        if (!functionsOfWorkbook.containsKey(workbook)) {
            functionsOfWorkbook.put(workbook, new HashMap<String, LiveExcelFunction>());
        }
    }
}
