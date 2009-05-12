package org.openl.poi.functions;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;

public class FunctionStatistics {
    private static final String FUNCTION_PRIORITIES_FILE = "test/functions/FunctionPriorities.xls";

    private List<Map<FunctionSupport, Integer>> statistics;
    private Map<String, Integer> functionPriorities;
    private int functionsCount = 0;

    public FunctionStatistics() {
        initPriorities();
        statistics = new ArrayList<Map<FunctionSupport, Integer>>();
        // for functions with priority "0"
        statistics.add(new HashMap<FunctionSupport, Integer>());
        // for functions with priority "1"
        statistics.add(new HashMap<FunctionSupport, Integer>());
        // for functions with priority "2"
        statistics.add(new HashMap<FunctionSupport, Integer>());
        // for functions with priority "3"
        statistics.add(new HashMap<FunctionSupport, Integer>());
        // for functions with priority "4"
        statistics.add(new HashMap<FunctionSupport, Integer>());
        // for functions with priority "5"
        statistics.add(new HashMap<FunctionSupport, Integer>());
        // for functions with priority "6"
        statistics.add(new HashMap<FunctionSupport, Integer>());
        // for functions without priority
        statistics.add(new HashMap<FunctionSupport, Integer>());
    }

    private void initPriorities() {
        functionPriorities = new HashMap<String, Integer>();
        InputStream is = null;
        try {
            is = new FileInputStream(FUNCTION_PRIORITIES_FILE);
            POIFSFileSystem fs = new POIFSFileSystem(is);
            HSSFWorkbook workbook = new HSSFWorkbook(fs);
            HSSFSheet sheet = workbook.getSheetAt(0);
            for (int i = 0; i <= sheet.getLastRowNum(); i++) {
                // function name in first column
                String functionName = sheet.getRow(i).getCell(0).getStringCellValue();
                // function priority in second column
                if (FunctionsRealizedChecker.isEmpty(sheet, i, (short) 1)) {
                    functionPriorities.put(functionName, 7);// without priority
                } else {
                    int functionPriority = (int) sheet.getRow(i).getCell(1).getNumericCellValue();
                    functionPriorities.put(functionName, functionPriority);
                }
            }
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

    public void registerFunction(String functionName, FunctionSupport functionStatus) {
        int priority = getFunctionPriority(functionName);
        int count = getFunctionsCountWithPriorityAndStatus(priority, functionStatus);
        statistics.get(priority).put(functionStatus, count + 1);
        functionsCount++;
    }

    public int getFunctionPriority(String functionName) {
        Integer priority = functionPriorities.get(functionName);
        if (priority == null) {
            return -1;
        } else {
            return priority;
        }
    }

    public int getFunctionsCount() {
        return functionsCount;
    }

    public int getFunctionsCountWithPriorityAndStatus(int priority, FunctionSupport functionStatus) {
        Integer count = statistics.get(priority).get(functionStatus);
        if (count == null) {
            return 0;
        } else {
            return count;
        }
    }
}
