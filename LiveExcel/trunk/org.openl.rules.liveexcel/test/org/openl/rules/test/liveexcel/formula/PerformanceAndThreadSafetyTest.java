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
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.junit.Test;
import static org.junit.Assert.*;
import org.openl.rules.liveexcel.formula.DeclaredFunctionSearcher;
import org.openl.rules.liveexcel.formula.LiveExcelFunctionsPack;
import org.openl.rules.liveexcel.hssf.usermodel.LiveExcelHSSFWorkbook;
import org.openl.rules.liveexcel.usermodel.LiveExcelWorkbook;
import org.openl.rules.liveexcel.usermodel.LiveExcelWorkbookFactory;

public class PerformanceAndThreadSafetyTest {

    public class CalculationThread implements Runnable {
        private HSSFFormulaEvaluator evaluator;
        private Cell cellToevaluate;
        private double expectedResult;

        public CalculationThread(HSSFFormulaEvaluator evaluator, Cell cellToevaluate, double expectedResult) {
            this.evaluator = evaluator;
            this.cellToevaluate = cellToevaluate;
            this.expectedResult = expectedResult;
        }

        public void run() {
            for (int i = 0; i < 10; i++) {
                assertTrue(expectedResult == evaluator.evaluate(cellToevaluate).getNumberValue());
            }
        }
    }

    private Workbook wb = parseWorkbook();
    List<Cell> cellsToEvaluate = new ArrayList<Cell>();
    List<Thread> cellCalculationThreads = new ArrayList<Thread>();
    List<Cell> udfs = new ArrayList<Cell>();
    List<Thread> udfCalculationThreads = new ArrayList<Thread>();

    @Test
    public void testPerformance() {
        findCellsToEvaluate();
        evaluateFormualas();
        evaluateUDFs();
    }

    @Test
    public void testThreadSafety() throws InterruptedException {
        createThreads();
        for (Thread thread : cellCalculationThreads) {
            thread.start();
        }
        for (Thread thread : udfCalculationThreads) {
            thread.start();
        }
        for (Thread thread : cellCalculationThreads) {
            thread.join();
        }
        for (Thread thread : udfCalculationThreads) {
            thread.join();
        }
    }

    private void createThreads() {
        findCellsToEvaluate();
        HSSFFormulaEvaluator evaluator = new HSSFFormulaEvaluator((LiveExcelHSSFWorkbook)wb);
        for (Cell cell : cellsToEvaluate) {
            cellCalculationThreads.add(new Thread(new CalculationThread(evaluator, cell, evaluator.evaluate(cell)
                    .getNumberValue())));
        }
        for (Cell cell : udfs) {
            evaluator.evaluateInCell(cell);
            udfCalculationThreads.add(new Thread(new CalculationThread(evaluator, cell, evaluator.evaluate(cell)
                    .getNumberValue())));
        }
    }

    private void evaluateUDFs() {
        int i;
        HSSFFormulaEvaluator evaluator = new HSSFFormulaEvaluator((LiveExcelHSSFWorkbook)wb);
        long startTime = System.currentTimeMillis();
        for (i = 0; i < 1000; i++) {
            for (Cell cell : udfs) {
                evaluator.evaluateFormulaCell(cell);
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
        HSSFFormulaEvaluator evaluator = new HSSFFormulaEvaluator((LiveExcelHSSFWorkbook)wb);
        long startTime = System.currentTimeMillis();
        for (i = 0; i < 1000; i++) {
            for (Cell cell : cellsToEvaluate) {
                evaluator.evaluateFormulaCell(cell);
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

    private Workbook parseWorkbook() {
        long startTime = System.currentTimeMillis();
        Workbook wb = getHSSFWorkbook("./test/resources/PerformanceTest.xls");
        DeclaredFunctionSearcher searcher = new DeclaredFunctionSearcher(wb);
        searcher.findFunctions();
        System.out.println("Time to parse workbook : " + (System.currentTimeMillis() - startTime) + " ms.");
        return wb;
    }

    private void findCellsToEvaluate() {
        cellsToEvaluate = new ArrayList<Cell>();
        udfs = new ArrayList<Cell>();
        for (int i = 0; i < wb.getNumberOfSheets(); i++) {
            Sheet sheet = wb.getSheetAt(i);
            for (Iterator<Row> rit = sheet.rowIterator(); rit.hasNext();) {
                Row r = rit.next();
                for (Iterator<Cell> cit = r.cellIterator(); cit.hasNext();) {
                    Cell c = cit.next();
                    if (c.getCellType() == HSSFCell.CELL_TYPE_FORMULA) {
                        DataFormatter dataFormatter = new DataFormatter();
                        String formattedValue = dataFormatter.formatCellValue(c);
                        if (!formattedValue.toUpperCase().startsWith(LiveExcelFunctionsPack.OL_DECLARATION_FUNCTION)) {
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

    public static Workbook getHSSFWorkbook(String fileName) {
        LiveExcelWorkbook workbook = null;
        InputStream is = null;
        try {
            is = new FileInputStream(fileName);
            
            workbook = LiveExcelWorkbookFactory.create(is, "SimpleExample");
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InvalidFormatException e) {
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
