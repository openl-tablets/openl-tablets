package org.openl.rules.test.liveexcel.formula;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;

import org.apache.poi.hssf.record.formula.eval.ErrorEval;
import org.apache.poi.hssf.record.formula.eval.NumberEval;
import org.apache.poi.hssf.record.formula.eval.ValueEval;
import org.apache.poi.hssf.record.formula.functions.FreeRefFunction;
import org.apache.poi.hssf.record.formula.udf.DefaultUDFFinder;
import org.apache.poi.hssf.record.formula.udf.UDFFinder;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.apache.poi.ss.formula.OperationEvaluationContext;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Workbook;
import org.openl.rules.liveexcel.formula.LiveExcelFunction;
import org.openl.rules.liveexcel.formula.LiveExcelFunctionsPack;

public class ExcelFunctionCalculator {
    public static class CellAddress {
        int row = 0;
        int column = 0;

        public CellAddress(int row, int column) {
            this.row = row;
            this.column = column;
        }

        public CellAddress(String address) {
            int index = 0;
            while (address.charAt(index) >= 'A' && address.charAt(index) <= 'Z') {
                column *= 26;
                column += 1 + address.charAt(index) - 'A';
                index++;
            }
            row = Integer.parseInt(address.substring(index));
            row--;
            column--;
        }

        public int getRow() {
            return row;
        }

        public int getColumn() {
            return column;
        }
    }

    HSSFWorkbook workbook;
    HSSFSheet sheet;

    public ExcelFunctionCalculator(String fileName) {
        InputStream is = null;
        try {
            is = new FileInputStream(fileName);
            POIFSFileSystem fs = new POIFSFileSystem(is);
            workbook = new HSSFWorkbook(fs);
            sheet = workbook.getSheetAt(0);
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

    // TODO: delete method
    private UDFFinder makeExampleOfUDF(Workbook wb) {
        LiveExcelFunctionsPack.instance().createUDFFinderLE(wb);
        LiveExcelFunctionsPack.instance().addUDF(wb, "base", new LiveExcelFunction() {
            public ValueEval execute(ValueEval[] args, OperationEvaluationContext ec) {
                if (args.length != 1) {
                    return ErrorEval.VALUE_INVALID;
                } else {
                    try {
                        return new NumberEval(((NumberEval) args[0]).getNumberValue() + 1);
                    } catch (Exception e) {
                        return ErrorEval.VALUE_INVALID;
                    }
                }
            }
        } );
        return LiveExcelFunctionsPack.instance().getUDFFinderLE(wb);
    }

    public Object calculateResult(CellAddress outputAddress, Object[] inputValues, CellAddress[] inputCellAddresses) {
        setValues(inputValues, inputCellAddresses);
        FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator(makeExampleOfUDF(workbook));
        evaluator.evaluateInCell(sheet.getRow(outputAddress.getRow()).getCell(outputAddress.getColumn()));
        return getCellValue(sheet.getRow(outputAddress.getRow()).getCell(outputAddress.getColumn()));
    }

    private void setValues(Object[] inputValues, CellAddress[] inputCellAddresses) {
        if (inputValues != null && inputCellAddresses != null) {
            for (int i = 0; i < inputValues.length; i++) {
                Cell cell = sheet.getRow(inputCellAddresses[i].getRow()).getCell(inputCellAddresses[i].getColumn());
                ExcelFunctionCalculator.setCellValue(cell, inputValues[i]);
            }
        }
    }

    public static void setCellValue(Cell cell, Object value) {
        if (value instanceof Number) {
            Number x = (Number) value;
            cell.setCellValue(x.doubleValue());

        } else if (value instanceof Date) {
            Date x = (Date) value;
            cell.setCellValue(x);
        } else
            cell.setCellValue(String.valueOf(value));
    }

    private Object getCellValue(Cell cell) {
        switch (cell.getCellType()) {
            case Cell.CELL_TYPE_BLANK:
                return null;
            case Cell.CELL_TYPE_BOOLEAN:
                return new Boolean(cell.getBooleanCellValue());
            case Cell.CELL_TYPE_FORMULA:
            case Cell.CELL_TYPE_NUMERIC:
                double value = cell.getNumericCellValue();
                return value == (int) value ? (Object) new Integer((int) value) : (Object) new Double(value);
            case Cell.CELL_TYPE_STRING:
                return cell.getStringCellValue();
            default:
                return "unknown type: " + cell.getCellType();
        }
    }

    /*
     * public static void main(String[] args) { ExcelFunctionCalculator
     * calculator = new ExcelFunctionCalculator("FOO.xls"); Object[] inputValues =
     * new Object[] { 10 }; CellAddress[] inputCellAddresses = new CellAddress[] {
     * new CellAddress("B1") };
     * System.out.println(calculator.calculateResult(new CellAddress("B2"),
     * inputValues, inputCellAddresses)); }
     */
}
