package org.openl.poi.functions;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;

public class FunctionStatistics {
    private static final String FUNCTION_PRIORITIES_FILE = "test/functions/FunctionPriorities.xls";

    private List<Map<FunctionSupportStatus, Integer>> statistics;
    private Map<String, Integer> functionPriorities;
    private int functionsCount = 0;

    public FunctionStatistics() {
        initPriorities();
        statistics = new ArrayList<Map<FunctionSupportStatus, Integer>>();
        // for functions with priority "0"
        statistics.add(new HashMap<FunctionSupportStatus, Integer>());
        // for functions with priority "1"
        statistics.add(new HashMap<FunctionSupportStatus, Integer>());
        // for functions with priority "2"
        statistics.add(new HashMap<FunctionSupportStatus, Integer>());
        // for functions with priority "3"
        statistics.add(new HashMap<FunctionSupportStatus, Integer>());
        // for functions with priority "4"
        statistics.add(new HashMap<FunctionSupportStatus, Integer>());
        // for functions with priority "5"
        statistics.add(new HashMap<FunctionSupportStatus, Integer>());
        // for functions with priority "6"
        statistics.add(new HashMap<FunctionSupportStatus, Integer>());
        // for functions without priority
        statistics.add(new HashMap<FunctionSupportStatus, Integer>());
        // for all functions
        statistics.add(new HashMap<FunctionSupportStatus, Integer>());
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

    public void registerFunction(String functionName, FunctionSupportStatus functionStatus) {
        int priority = getFunctionPriorityByName(functionName);
        int count = getFunctionsCountWithPriorityAndStatus(priority, functionStatus);
        statistics.get(8).put(functionStatus, getFunctionsCountWithPriorityAndStatus(8, functionStatus) + 1);
        statistics.get(priority).put(functionStatus, count + 1);
        functionsCount++;
    }

    public int getFunctionPriorityByName(String functionName) {
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

    public int getFunctionsCountWithPriority(int priority) {
        int count = 0;
        Map<FunctionSupportStatus, Integer> statistic = statistics.get(priority);
        for (FunctionSupportStatus status : statistic.keySet()) {
            count += getFunctionsCountWithPriorityAndStatus(priority, status);
        }
        return count;
    }

    public int getFunctionsCountWithPriorityAndStatus(int priority, FunctionSupportStatus functionStatus) {
        Integer count = statistics.get(priority).get(functionStatus);
        if (count == null) {
            return 0;
        } else {
            return count;
        }
    }

    /*public void save() throws Exception {
        InputStream is = null;
        OutputStream out = null;
        try {
            is = new FileInputStream(FUNCTION_STATISTICS_FILE);
            POIFSFileSystem fs = new POIFSFileSystem(is);
            HSSFWorkbook workbook = new HSSFWorkbook(fs);
            HSSFSheet sheet = workbook.getSheetAt(0);
            fillSheetWithStatistics(sheet);
            workbook.write(out = new FileOutputStream(FUNCTION_STATISTICS_FILE));
        } catch (IOException e) {
            throw e;
        } finally {
            try {
                if (is != null)
                    is.close();
            } catch (Exception e) {
                throw e;
            }
            try {
                if (out != null)
                    out.close();
            } catch (Exception e) {
                throw e;
            }
        }
    }*/

    // analog of method of org.openl.rules.table.xls.XlsSheetGridModel
    public static Cell createNewCell(HSSFSheet sheet, int colTo, int rowTo) {
        Row row = sheet.getRow(rowTo);
        if (row == null)
            row = sheet.createRow(rowTo);

        Cell cell = row.getCell(colTo);
        if (cell == null)
            cell = row.createCell(colTo);
        return cell;
    }

    // analog of method of org.openl.rules.table.xls.XlsSheetGridModel
    public static void setCellValue(HSSFSheet sheet, int row, int column, Object value) {
        if (value == null)
            return;

        Cell cell = createNewCell(sheet, column, row);

        if (value instanceof Number) {
            Number x = (Number) value;
            cell.setCellValue(x.doubleValue());

        } else if (value instanceof Date) {
            Date x = (Date) value;
            cell.setCellValue(x);
        } else
            cell.setCellValue(String.valueOf(value));
    }

    public void fillSheetWithStatistics(HSSFSheet sheet) {
        final int ONE_PRIORITY_BLOCK_SIZE = 12;
        final int FIRST_PRIORITY_BLOCK_ROW_INDEX = 2;
        int startRow = FIRST_PRIORITY_BLOCK_ROW_INDEX;
        fillStatisticsOfOnePriorityBlock(sheet, 8, startRow);
        setCellValue(sheet, 0, 1, functionsCount);
        for (int i = statistics.size() - 3; i >= 0; i--) {
            startRow = FIRST_PRIORITY_BLOCK_ROW_INDEX + ONE_PRIORITY_BLOCK_SIZE * (statistics.size() - 2 - i);
            fillStatisticsOfOnePriorityBlock(sheet, i, startRow);
        }
        startRow = FIRST_PRIORITY_BLOCK_ROW_INDEX + ONE_PRIORITY_BLOCK_SIZE * (statistics.size() - 1);
        fillStatisticsOfOnePriorityBlock(sheet, 7, startRow);
    }

    private void fillStatisticsOfOnePriorityBlock(HSSFSheet sheet, int priority, int startRow) {
        final int COLUMN_INDEX_OF_CELL_OF_DATA = 1;
        if (priority == 7) {
            setCellValue(sheet, startRow, COLUMN_INDEX_OF_CELL_OF_DATA, "not specified");
        } else if (priority == 8) {
            setCellValue(sheet, startRow, COLUMN_INDEX_OF_CELL_OF_DATA, "all functions");
        } else {
            setCellValue(sheet, startRow, COLUMN_INDEX_OF_CELL_OF_DATA, priority);
        }
        setCellValue(sheet, startRow + 1, COLUMN_INDEX_OF_CELL_OF_DATA, getFunctionsCountWithPriority(priority));
        setCellValue(sheet, startRow + 2, COLUMN_INDEX_OF_CELL_OF_DATA, getFunctionsCountWithPriorityAndStatus(
                priority, FunctionSupportStatus.SUPPORTED));
        setCellValue(sheet, startRow + 3, COLUMN_INDEX_OF_CELL_OF_DATA, getFunctionsCountWithPriorityAndStatus(
                priority, FunctionSupportStatus.TESTED_WITH_ERRORS));
        setCellValue(sheet, startRow + 4, COLUMN_INDEX_OF_CELL_OF_DATA, getFunctionsCountWithPriorityAndStatus(
                priority, FunctionSupportStatus.NOT_TESTED));
        setCellValue(sheet, startRow + 5, COLUMN_INDEX_OF_CELL_OF_DATA, getFunctionsCountWithPriorityAndStatus(
                priority, FunctionSupportStatus.NON_IMPLEMENTED));
    }
}
