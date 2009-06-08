package org.openl.rules.test.liveexcel.formula;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFFormulaEvaluator;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.junit.Test;
import org.openl.rules.liveexcel.formula.DeclaredFunctionSearcher;

public class PerformanceTest {

    private HSSFWorkbook wb;
    List<Cell> cellsToEvaluate = new ArrayList<Cell>();
    List<Cell> udfs = new ArrayList<Cell>();

    @Test
    public void test() {
        parseWorkbook();
        findCellsToEvaluate();
        evaluateFormualas();
        evaluateUDFs();
    }

    private void evaluateUDFs() {
        int i;
        HSSFFormulaEvaluator evaluator = new HSSFFormulaEvaluator(wb);
        long startTime = System.currentTimeMillis();
        for (i = 0; i < 100000; i++) {
            for (Cell cell : udfs) {
                evaluator.evaluateInCell(cell);
            }
            if (i == 0) {
                long resultTime = System.currentTimeMillis() - startTime;
                System.out.println("Time to evaluate UDFs of workbook the first time (" + udfs.size() + " formulas): "
                        + resultTime + " ms.");
                System.out.println(resultTime * 1.0 / udfs.size() + " ms per formula.");
            }
            evaluator.clearAllCachedResultValues();
        }
        System.out.println("Time to evaluate UDFs of workbook " + i + " times (" + udfs.size() + " formulas): "
                + (System.currentTimeMillis() - startTime) + " ms.");
    }

    private void evaluateFormualas() {
        int i;
        HSSFFormulaEvaluator evaluator = new HSSFFormulaEvaluator(wb);
        long startTime = System.currentTimeMillis();
        for (i = 0; i < 100000; i++) {
            for (Cell cell : cellsToEvaluate) {
                evaluator.evaluateInCell(cell);
            }
            if (i == 0) {
                long resultTime = System.currentTimeMillis() - startTime;
                System.out.println("Time to evaluate workbook the first time (" + cellsToEvaluate.size()
                        + " formulas): " + resultTime + " ms.");
                System.out.println(resultTime * 1.0 / cellsToEvaluate.size() + " ms per formula.");
            }
            evaluator.clearAllCachedResultValues();
        }
        System.out.println("Time to evaluate workbook " + i + " times (" + cellsToEvaluate.size() + " formulas): "
                + (System.currentTimeMillis() - startTime) + " ms.");
    }

    private void parseWorkbook() {
        long startTime = System.currentTimeMillis();
        wb = getHSSFWorkbook("./test/resources/PerformanceTest.xls");
        DeclaredFunctionSearcher searcher = new DeclaredFunctionSearcher(wb);
        searcher.findFunctions();
        System.out.println("Time to parse workbook : " + (System.currentTimeMillis() - startTime) + " ms.");
    }

    private void findCellsToEvaluate() {
        cellsToEvaluate = new ArrayList<Cell>();
        udfs = new ArrayList<Cell>();
        for (int i = 0; i < wb.getNumberOfSheets(); i++) {
            HSSFSheet sheet = wb.getSheetAt(i);
            for (Iterator<Row> rit = sheet.rowIterator(); rit.hasNext();) {
                Row r = rit.next();
                for (Iterator<Cell> cit = r.cellIterator(); cit.hasNext();) {
                    Cell c = cit.next();
                    if (c.getCellType() == HSSFCell.CELL_TYPE_FORMULA) {
                        DataFormatter dataFormatter = new DataFormatter();
                        String formattedValue = dataFormatter.formatCellValue(c);
                        if (!formattedValue.toUpperCase().startsWith(DeclaredFunctionSearcher.OL_DECLARATION_FUNCTION)) {
                            if ("UDFS".equals(sheet.getSheetName())) {
                                if (wb.getUserDefinedFunction(getFunctionName(formattedValue)) != null) {
                                    udfs.add(c);
                                }
                            } else {
                                cellsToEvaluate.add(c);
                            }
                        }
                    }
                }
            }
        }
    }

    private static String getFunctionName(String formula) {
        return formula.substring(0, formula.indexOf('('));
    }

    public static HSSFWorkbook getHSSFWorkbook(String fileName) {
        HSSFWorkbook workbook = null;
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
        return workbook;
    }
}
