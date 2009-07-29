package org.openl.rules.test.liveexcel;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.junit.Test;
import org.openl.rules.liveexcel.usermodel.LiveExcelWorkbookFactory;
import org.openl.util.benchmark.Benchmark;
import org.openl.util.benchmark.BenchmarkInfo;
import org.openl.util.benchmark.BenchmarkUnit;

import static org.junit.Assert.*;

public class FormulaEvaluationTest {

    private static final String FORMULA_TEST = "./test/resources/EvaluationTest/FormulasTest.xls";
    private static final String BENCHMARK_FILE = "./test/statistics/Benchmarks.xlsx";

    private static class POIEvauation extends BenchmarkUnit {
        private Sheet sheet;
        private List<Cell> formulaCells;

        public POIEvauation(Sheet sheet) {
            this.sheet = sheet;
            formulaCells = findFormulaCells(sheet);
        }

        private List<Cell> findFormulaCells(Sheet sheet) {
            List<Cell> result = new ArrayList<Cell>();
            for (Iterator<Row> rit = sheet.rowIterator(); rit.hasNext();) {
                Row r = rit.next();
                for (Iterator<Cell> cit = r.cellIterator(); cit.hasNext();) {
                    Cell c = cit.next();
                    if (c.getCellType() == HSSFCell.CELL_TYPE_FORMULA) {
                        result.add(c);
                    }
                }
            }
            return result;
        }

        protected String getNameSpecial() {
            return sheet.getSheetName() + "(Excel)";
        }

        @Override
        protected void run() throws Exception {
            FormulaEvaluator evaluator = sheet.getWorkbook().getCreationHelper().createFormulaEvaluator();
            for (Cell cell : formulaCells) {
                evaluator.evaluateFormulaCell(cell);
            }
        }
    }

    private static class OneRelationJavaEvaluation extends BenchmarkUnit {
        private long[][] evaluationArray;
        private int height, width;

        public OneRelationJavaEvaluation(int width, int height) {
            this.width = width;
            this.height = height;
            evaluationArray = new long[width][height + 1];
            for (int i = 0; i < width; i++) {
                evaluationArray[i][0] = i + 1;
            }
        }

        protected String getNameSpecial() {
            return "OneRelationSum" + width + "x" + height + "(Java)";
        }

        @Override
        protected void run() throws Exception {
            for (int row = 1; row < height + 1; row++) {
                for (int column = 0; column < width; column++) {
                    evaluationArray[column][row] = evaluationArray[column][row - 1] + 1;
                }
            }
        }
    }

    private static class TwoRelationJavaEvaluation extends BenchmarkUnit {
        private long[][] evaluationArray;
        private int height, width;

        public TwoRelationJavaEvaluation(int width, int height) {
            this.width = width;
            this.height = height;
            evaluationArray = new long[width + 1][height + 1];
            for (int i = 0; i < width + 1; i++) {
                evaluationArray[i][0] = i + 1;
            }
            for (int i = 0; i < height + 1; i++) {
                evaluationArray[0][i] = i + 1;
            }
        }

        protected String getNameSpecial() {
            return "TwoRelationSum" + width + "x" + height + "(Java)";
        }

        @Override
        protected void run() throws Exception {
            for (int row = 1; row < height + 1; row++) {
                for (int column = 1; column < width + 1; column++) {
                    evaluationArray[column][row] = evaluationArray[column][row - 1]
                            + evaluationArray[column - 1][row - 1];
                }
            }
        }
    }

    private static class EvaluationStatisticsSaver {
        private static int MEMORY_USED_ROW = 11;
        private static int JAVA_ONE_RELATION_ROW = 8;
        private static int JAVA_TWO_RELATIONS_ROW = 9;
        private static int POI_ONE_RELATION_ROW = 4;
        private static int POI_TWO_RELATIONS_ROW = 5;
        private static int COLUMN_25x25 = 1;
        private static int COLUMN_25x250 = 2;
        private static int COLUMN_25x1000 = 3;

        private int getColumnToWrite(String benchmarkName) {
            if (benchmarkName.lastIndexOf("2500") != -1) {
                return COLUMN_25x1000;
            } else if (benchmarkName.lastIndexOf("250") != -1) {
                return COLUMN_25x250;
            } else {
                return COLUMN_25x25;
            }
        }

        private int getRowToWrite(String benchmarkName) {
            if (benchmarkName.lastIndexOf("Java") != -1) {
                if (benchmarkName.startsWith("One")) {
                    return JAVA_ONE_RELATION_ROW;
                } else {
                    return JAVA_TWO_RELATIONS_ROW;
                }
            } else {
                if (benchmarkName.startsWith("One")) {
                    return POI_ONE_RELATION_ROW;
                } else {
                    return POI_TWO_RELATIONS_ROW;
                }
            }
        }

        private void saveStatistic(String benchmarkName, BenchmarkInfo benchmarkInfo, Sheet evaluationBenchmarkSheet) {
            int rowIndex = getRowToWrite(benchmarkName);
            int columnIndex = getColumnToWrite(benchmarkName);
            evaluationBenchmarkSheet.getRow(rowIndex).getCell(columnIndex).setCellValue(benchmarkInfo.avg());
        }

        public void save(Map<String, BenchmarkInfo> statistic, long memoryUsed) {
            OutputStream out = null;
            InputStream in = null;
            try {
                Workbook workbook = LiveExcelWorkbookFactory.create((in = new FileInputStream(BENCHMARK_FILE)), null);
                Sheet evaluationBenchmarkSheet = workbook.getSheetAt(0);
                evaluationBenchmarkSheet.getRow(MEMORY_USED_ROW).getCell(1).setCellValue(memoryUsed);
                for (String benchmarkName : statistic.keySet()) {
                    saveStatistic(benchmarkName, statistic.get(benchmarkName), evaluationBenchmarkSheet);
                }
                workbook.write((out = new FileOutputStream(BENCHMARK_FILE)));
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try {
                    if (in != null)
                        in.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                try {
                    if (out != null)
                        out.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static long getUsedMemorySize() {
        return Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
    }
    
    @Test
    public void testEvaluation(){
        long initialUsedMemory = getUsedMemorySize();

        InputStream in = null;

        try {
            Workbook workbook = LiveExcelWorkbookFactory.create((in = new FileInputStream(FORMULA_TEST)), null);
            BenchmarkUnit[] bu = { new POIEvauation(workbook.getSheetAt(0)), new OneRelationJavaEvaluation(25, 25),
                    new POIEvauation(workbook.getSheetAt(1)), new TwoRelationJavaEvaluation(25, 25),
                    new POIEvauation(workbook.getSheetAt(2)), new OneRelationJavaEvaluation(25, 250),
                    new POIEvauation(workbook.getSheetAt(3)), new TwoRelationJavaEvaluation(25, 250),
                    new POIEvauation(workbook.getSheetAt(4)), new OneRelationJavaEvaluation(25, 2500),
                    new POIEvauation(workbook.getSheetAt(5)), new TwoRelationJavaEvaluation(25, 2500) };
            Map<String, BenchmarkInfo> res = new Benchmark(bu).measureAll(1000);

            new EvaluationStatisticsSaver().save(res, getUsedMemorySize() - initialUsedMemory);
            assertTrue(true);
        } catch (Exception e) {
            e.printStackTrace();
            assertTrue(false);
        } finally {
            try {
                if (in != null)
                    in.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
