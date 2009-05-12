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

public class FunctionsRealizedChecker {
    private static final String FUNCTIONS_OUTPUT_FILE = "test/functions/CheckedFunctions.xls";

    HSSFSheet resultSheet;
    private HSSFWorkbook resultWorkbook;
    private FunctionStatistics statistics = new FunctionStatistics();

    private HSSFFormulaEvaluator evaluator;
    private HSSFWorkbook inputWorkbook;

    private void init() {
        evaluator = new HSSFFormulaEvaluator(inputWorkbook);

        if (resultWorkbook == null) {
            resultWorkbook = new HSSFWorkbook();
            resultSheet = resultWorkbook.createSheet("functions_checked");
        }
    }

    public void analyze(String fileName) throws Exception {
        InputStream is = null;
        OutputStream out = null;
        try {
            is = new FileInputStream("test/functions/tests/" + fileName);
            POIFSFileSystem fs = new POIFSFileSystem(is);
            inputWorkbook = new HSSFWorkbook(fs);
            init();
            String functionsGroup = fileName.substring(0, fileName.lastIndexOf('.'));
            System.out.println(functionsGroup);

            for (int i = 0; i < inputWorkbook.getNumberOfSheets(); i++) {
                HSSFSheet currentSheet = inputWorkbook.getSheetAt(i);
                FunctionSupport functionStatus = checkFunction(currentSheet);
                addCheckedFuction(currentSheet.getSheetName(), functionStatus);
            }
            resultWorkbook.write(out = new FileOutputStream(FUNCTIONS_OUTPUT_FILE));
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

    private void addCheckedFuction(String functionName, FunctionSupport functionStatus) {
        HSSFRow row = resultSheet.createRow(statistics.getFunctionsCount());
        // in first column function name
        row.createCell(0).setCellValue(functionName);
        if (functionStatus != null) {
            fillCellWithColor(row.getCell(0), getColor(functionStatus));
            // in second column function status
            row.createCell(1).setCellValue(functionStatus.getMessage());
            // in third column and next columns errors that occurs during the
            // evaluation
            for (int i = 0; i < functionStatus.getErrors().size(); i++) {
                row.createCell(2 + i).setCellValue(functionStatus.getErrors().get(i));
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

    private HSSFColor getColor(FunctionSupport functionSupport) {
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

    private FunctionSupport checkFunction(HSSFSheet functionTestsSheet) {
        int numberOfTests = 0;
        int numberOfErrors = 0;
        List<String> errorList = new ArrayList<String>();
        int formulaRow = getFirstFormulaIndex(functionTestsSheet);
        if (formulaRow == -1) {
            return FunctionSupport.NOT_TESTED;
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
                FunctionSupport functionStatus = FunctionSupport.NON_IMPLEMENTED;
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
            FunctionSupport functionStatus = FunctionSupport.TESTED_WITH_ERRORS;
            functionStatus.setMessage(numberOfErrors + " of " + numberOfTests + " tests failed.");
            functionStatus.setErrors(errorList);
            return functionStatus;
        }
        return FunctionSupport.SUPPORTED;
    }

    // analog of method of org.openl.rules.table.xls.XlsSheetGridModel
    public static Object getCellValue(HSSFSheet sheet, int row, int column) {
        Cell cell = sheet.getRow(row).getCell(column);
        switch (cell.getCellType()) {
            case Cell.CELL_TYPE_BLANK:
                return null;
            case Cell.CELL_TYPE_BOOLEAN:
                return new Boolean(cell.getBooleanCellValue());
            case Cell.CELL_TYPE_NUMERIC:
                // try{
                double value = cell.getNumericCellValue();
                return value == (int) value ? (Object) new Integer((int) value) : (Object) new Double(value);
                // }catch (IllegalStateException e) {
                // return cell.getErrorCellValue();
                // }
            case Cell.CELL_TYPE_STRING:
                return cell.getStringCellValue();
            default:
                return "unknown type: " + cell.getCellType();
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
        final short columnIndexOfFormulaLabel = 0;
        while ((!isEmpty(functionTestsSheet, rowNumber, columnIndexOfFormulaLabel) && !"Formula".equals(getCellValue(
                functionTestsSheet, rowNumber, columnIndexOfFormulaLabel)))
                || (isEmpty(functionTestsSheet, rowNumber, columnIndexOfFormulaLabel) && isPartOfTheMergedRegion(
                        functionTestsSheet, rowNumber, columnIndexOfFormulaLabel))) {
            rowNumber++;
        }
        if (isEmpty(functionTestsSheet, rowNumber, (short) 0)) {
            return -1;
        }
        return rowNumber + 1;
    }

    public static void main(String[] args) throws Exception {
        FunctionsRealizedChecker checker = new FunctionsRealizedChecker();
        checker.analyze("Add-in_and_automation_functions.xls");
        checker.analyze("Database_and_list_management.xls");
        checker.analyze("Date_and_time.xls");
        checker.analyze("Engineering.xls");
        checker.analyze("Financial.xls");
        checker.analyze("Information.xls");
        checker.analyze("Logical.xls");
        checker.analyze("Lookup_and_reference.xls");
        checker.analyze("Math_and_trigonometry.xls");
        checker.analyze("Statistical.xls");
        checker.analyze("Text_and_data.xls");
    }
}
