package org.openl.poi.functions;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.hssf.usermodel.HSSFFormulaEvaluator;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.hssf.util.HSSFColor;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.apache.poi.ss.formula.eval.NotImplementedException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.hssf.util.Region;
import org.junit.Assert;

public class FunctionsRealizedChecker {
    private static final String FUNCTION_STATISTICS_FILE = "test/functions/Statistics.xls";

    HSSFSheet resultSheet;
    private HSSFWorkbook resultWorkbook;
    private FunctionStatistics statistics = new FunctionStatistics();

    private HSSFFormulaEvaluator evaluator;
    private HSSFWorkbook inputWorkbook;

    InputStream is = null;

    private void init() {
        evaluator = new HSSFFormulaEvaluator(inputWorkbook);

        if (resultWorkbook == null) {
            try {
                is = new FileInputStream(FUNCTION_STATISTICS_FILE);
                POIFSFileSystem fs = new POIFSFileSystem(is);
                resultWorkbook = new HSSFWorkbook(fs);
            } catch (IOException e) {
                e.printStackTrace();
            }
            resultSheet = resultWorkbook.getSheetAt(1);
        }
    }

    public void analyze(String fileName) throws Exception {
        InputStream is = null;
        try {
            is = new FileInputStream("test/functions/tests/" + fileName);
            POIFSFileSystem fs = new POIFSFileSystem(is);
            inputWorkbook = new HSSFWorkbook(fs);
            init();
            String functionsGroup = fileName.substring(0, fileName.lastIndexOf('.'));
            System.out.println(functionsGroup);

            for (int i = 0; i < inputWorkbook.getNumberOfSheets(); i++) {
                HSSFSheet currentSheet = inputWorkbook.getSheetAt(i);
                FunctionSupportStatus functionStatus = checkFunction(currentSheet);
                String functionName = currentSheet.getSheetName();
                int priority = statistics.getFunctionPriorityByName(functionName);
                addCheckedFuction(currentSheet.getSheetName(), functionStatus, priority);
                testJUnit(currentSheet, functionStatus);
            }
        } catch (IOException e) {
            throw e;
        } finally {
            try {
                if (is != null)
                    is.close();
            } catch (Exception e) {
                throw e;
            }
        }
    }

    private void testJUnit(HSSFSheet sheet, FunctionSupportStatus functionStatus) {
        // if cell (0,0) contains "Junit test" then it must be completely
        // supported by poi and tested by JUnit
        if ("Junit test".equals(getCellValue(sheet, 0, 0))) {
            System.out.println("Tetsted function : " + sheet.getSheetName());
            Assert.assertTrue(functionStatus == FunctionSupportStatus.SUPPORTED);
        }
    }

    private void addCheckedFuction(String functionName, FunctionSupportStatus functionStatus, int priority) {
        HSSFRow row = resultSheet.createRow(statistics.getFunctionsCount() + 1);
        // in first column function name
        HSSFCell priorityCell = row.createCell(0);
        priorityCell.getCellStyle().setAlignment(HSSFCellStyle.ALIGN_CENTER);
        priorityCell.setCellValue(priority);
        row.createCell(1).setCellValue(functionName);
        if (functionStatus != null) {
            fillCellWithColor(row.getCell(1), getColor(functionStatus));
            // in second column function status
            row.createCell(2).setCellValue(functionStatus.getMessage());
            // in third column and next columns errors that occurs during the
            // evaluation
            for (int i = 0; i < functionStatus.getErrors().size(); i++) {
                row.createCell(3 + i).setCellValue(functionStatus.getErrors().get(i));
            }
        }
        statistics.registerFunction(functionName, functionStatus);
    }

    private void fillCellWithColor(HSSFCell cell, HSSFColor color) {
        HSSFCellStyle cellStyle = resultWorkbook.createCellStyle();
        cellStyle.setFillForegroundColor(color.getIndex());
        cellStyle.setFillPattern(HSSFCellStyle.SOLID_FOREGROUND);

        cell.setCellStyle(cellStyle);
    }

    private HSSFColor getColor(FunctionSupportStatus functionSupport) {
        switch (functionSupport) {
            case NON_IMPLEMENTED:
                return new HSSFColor.RED();
            case NOT_TESTED:
                return new HSSFColor.YELLOW();
            case TESTED_WITH_ERRORS:
                return new HSSFColor.ORANGE();
            case SUPPORTED:
                return new HSSFColor.GREEN();
        }
        return new HSSFColor.WHITE();
    }

    private FunctionSupportStatus checkFunction(HSSFSheet functionTestsSheet) {
        int numberOfTests = 0;
        int numberOfErrors = 0;
        List<String> errorList = new ArrayList<String>();
        int formulaRow = getFirstFormulaIndex(functionTestsSheet);
        if (formulaRow == -1) {
            return FunctionSupportStatus.NOT_TESTED;
        }
        while (!isEmpty(functionTestsSheet, formulaRow, (short) 0)) {
            try {
                numberOfTests++;
                HSSFCell currentFormulaCell = functionTestsSheet.getRow(formulaRow).getCell(0);
                Object previousResult = getCellValue(functionTestsSheet, formulaRow, 0);
                evaluator.evaluate(currentFormulaCell);
                if (!previousResult.equals(getCellValue(functionTestsSheet, formulaRow, 0))) {
                    throw new RuntimeException(previousResult + " not equals "
                            + getCellValue(functionTestsSheet, formulaRow, 0));
                }
            } catch (NotImplementedException e) {
                FunctionSupportStatus functionStatus = FunctionSupportStatus.NON_IMPLEMENTED;
                String operationNonImplemented = e.getCause().getMessage();
                if (operationNonImplemented.startsWith("org.apache.poi.hssf.record.formula.functions.")) {
                    functionStatus.setMessage("Function is not yet implemented: "
                            + operationNonImplemented.substring(operationNonImplemented.lastIndexOf('.') + 1));
                } else {
                    functionStatus.setMessage("Non-implemented function : " + e.getCause().getMessage());
                }
                return functionStatus;
            } catch (Exception e) {
                errorList.add(e.getMessage());
                numberOfErrors++;
            }
            formulaRow++;
        }
        if (numberOfErrors > 0) {
            FunctionSupportStatus functionStatus = FunctionSupportStatus.TESTED_WITH_ERRORS;
            functionStatus.setMessage(numberOfErrors + " of " + numberOfTests + " tests failed.");
            functionStatus.setErrors(errorList);
            return functionStatus;
        }
        return FunctionSupportStatus.SUPPORTED;
    }

    // analog of method of org.openl.rules.table.xls.XlsSheetGridModel
    public static Object getCellValue(HSSFSheet sheet, int row, int column) {
        if (isEmpty(sheet, row, (short) column)) {
            return null;
        }
        Cell cell = sheet.getRow(row).getCell(column);
        switch (cell.getCellType()) {
            default:
                return "unknown type: " + cell.getCellType();
            case Cell.CELL_TYPE_BLANK:
                return null;
            case Cell.CELL_TYPE_BOOLEAN:
                return new Boolean(cell.getBooleanCellValue());
            case Cell.CELL_TYPE_ERROR:
                return cell.getErrorCellValue();
            case Cell.CELL_TYPE_STRING:
                return cell.getStringCellValue();
            case Cell.CELL_TYPE_FORMULA:
            case Cell.CELL_TYPE_NUMERIC:
                try {
                    double value = cell.getNumericCellValue();
                    return value == (int) value ? (Object) new Integer((int) value) : (Object) new Double(value);
                } catch (Exception exceptionOfNumeric) {
                    try {
                        // some formulas returns string (="string value")
                        return cell.getStringCellValue();
                    } catch (Exception exceptionOfString) {
                        return cell.getErrorCellValue();
                    }
                }
        }
    }

    // analog of method of org.openl.rules.table.AGridModel
    public static boolean isPartOfTheMergedRegion(HSSFSheet sheet, int row, short column) {
        int nregions = sheet.getNumMergedRegions();
        for (int i = 0; i < nregions; i++) {
            Region reg = sheet.getMergedRegionAt(i);
            if (reg.contains(row, column)) {
                return true;
            }
        }
        return false;
    }

    // analog of method of org.openl.rules.table.xls.XlsSheetGridModel
    public static boolean isEmpty(HSSFSheet sheet, int rowIndex, short columnIndex) {
        Row row = sheet.getRow(rowIndex);
        if (row == null) {
            return true;
        }
        Cell cell = row.getCell(columnIndex);
        if (cell == null) {
            return true;
        }
        if (cell.getCellType() == Cell.CELL_TYPE_BLANK) {
            return true;
        }
        if (cell.getCellType() == Cell.CELL_TYPE_STRING) {

            String v = cell.getStringCellValue();
            return v == null || v.trim().length() == 0;
        }
        return false;
    }

    private int getFirstFormulaIndex(HSSFSheet functionTestsSheet) {
        int rowNumber = 0;
        final short COLUMN_INDEX_OF_FORMILA_LABEL = 0;
        while ((!isEmpty(functionTestsSheet, rowNumber, COLUMN_INDEX_OF_FORMILA_LABEL) && !"Formula"
                .equals(getCellValue(functionTestsSheet, rowNumber, COLUMN_INDEX_OF_FORMILA_LABEL)))
                || (isEmpty(functionTestsSheet, rowNumber, COLUMN_INDEX_OF_FORMILA_LABEL) && isPartOfTheMergedRegion(
                        functionTestsSheet, rowNumber, COLUMN_INDEX_OF_FORMILA_LABEL))) {
            rowNumber++;
        }
        if (isEmpty(functionTestsSheet, rowNumber, (short) 0)) {
            return -1;
        }
        return rowNumber + 1;
    }

    public void saveResults() throws Exception {
        OutputStream out = null;
        try {
            HSSFSheet sheet = resultWorkbook.getSheetAt(0);
            statistics.fillSheetWithStatistics(sheet);
            resultWorkbook.write(out = new FileOutputStream(FUNCTION_STATISTICS_FILE));
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
    }

    /*public void saveResults() throws Exception {
        OutputStream out = null;
        try {
            resultWorkbook.write(out = new FileOutputStream(FUNCTIONS_OUTPUT_FILE));
        } catch (IOException e) {
            throw e;
        } finally {
            try {
                if (out != null)
                    out.close();
            } catch (Exception e) {
                throw e;
            }
        }
        statistics.save();
    }*/

    public void testAllFunctions() throws Exception {
        try {
            analyze("Add-in_and_automation_functions.xls");
            analyze("Cube.xls");
            analyze("Database_and_list_management.xls");
            analyze("Date_and_time.xls");
            analyze("Engineering.xls");
            analyze("Financial.xls");
            analyze("Information.xls");
            analyze("Logical.xls");
            analyze("Lookup_and_reference.xls");
            analyze("Math_and_trigonometry.xls");
            analyze("Statistical.xls");
            analyze("Text_and_data.xls");
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            saveResults();
        }
    }

    public static void main(String[] args) throws Exception {
        new FunctionsRealizedChecker().testAllFunctions();
    }
}
