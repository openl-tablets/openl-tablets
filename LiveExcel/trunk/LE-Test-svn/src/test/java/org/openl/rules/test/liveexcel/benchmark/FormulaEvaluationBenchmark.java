package org.openl.rules.test.liveexcel.benchmark;

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
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.junit.Test;
import org.openl.rules.liveexcel.usermodel.LiveExcelWorkbookFactory;
import org.openl.util.benchmark.Benchmark;
import org.openl.util.benchmark.BenchmarkInfo;
import org.openl.util.benchmark.BenchmarkUnit;
import org.openl.util.benchmark.BenchmarkWithMemory;

import static org.junit.Assert.*;

public class FormulaEvaluationBenchmark {

    private static final String FORMULA_TEST_XLSX = "./test/resources/EvaluationTest/FormulasTest.xlsx";
    private static final String FORMULA_TEST_XLS = "./test/resources/EvaluationTest/FormulasTest.xls";
    private static final String BENCHMARK_FILE = "./test/statistics/Benchmarks.xlsx";

    public static abstract class VerticalTableBenchmarkUnit extends BenchmarkUnit {
        public abstract int getHeight();
    }

    private static class POIEvauation extends VerticalTableBenchmarkUnit {
        private Sheet sheet;
        private List<Cell> formulaCells;
        private FormulaEvaluator evaluator;

        public POIEvauation(Sheet sheet) {
            this.sheet = sheet;
            formulaCells = findFormulaCells(sheet);
            evaluator = sheet.getWorkbook().getCreationHelper().createFormulaEvaluator();
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
            if (sheet instanceof XSSFSheet) {
                return sheet.getSheetName() + "(XLSX)";
            } else {
                return sheet.getSheetName() + "(XLS)";
            }
        }

        @Override
        protected void run() throws Exception {
            for (Cell cell : formulaCells) {
                evaluator.evaluateFormulaCell(cell);
            }
        }

        public String[] performAfter() {
            evaluator.clearAllCachedResultValues();
            return new String[0];
        }

        public int getHeight() {
            return formulaCells.size() / 25;
        }
    }

    private static class OneRelationJavaEvaluation extends VerticalTableBenchmarkUnit {
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
            getCell(width - 1, height);
            for (int row = 1; row < height + 1; row++) {
                for (int column = 0; column < width; column++) {
                    getCell(column, row);
                }
            }
        }

        private long getCell(int x, int y) {
            if (evaluationArray[x][y] != 0) {
                return evaluationArray[x][y];
            } else {
                return (evaluationArray[x][y] = 1 + getCell(x, y - 1));
            }
        }

        public String[] performAfter() {
            for (int row = 1; row < height + 1; row++) {
                for (int column = 0; column < width; column++) {
                    evaluationArray[column][row] = 0;
                }
            }
            return new String[0];
        }

        public int getHeight() {
            return height;
        }
    }

    private static class TwoRelationJavaEvaluation extends VerticalTableBenchmarkUnit {
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
            getCell(width, height);
            for (int row = 1; row < height + 1; row++) {
                for (int column = 1; column < width + 1; column++) {
                    getCell(column, row);
                }
            }
        }

        private long getCell(int x, int y) {
            if (evaluationArray[x][y] != 0) {
                return evaluationArray[x][y];
            } else {
                return (evaluationArray[x][y] = getCell(x - 1, y) + getCell(x, y - 1));
            }
        }

        public String[] performAfter() {
            for (int row = 1; row < height + 1; row++) {
                for (int column = 1; column < width + 1; column++) {
                    evaluationArray[column][row] = 0;
                }
            }
            return new String[0];
        }

        public int getHeight() {
            return height;
        }
    }

    private static class EvaluationStatisticsSaver {
        private static int MEMORY_USED_ROW = 11;
        private static int JAVA_ONE_RELATION_ROW = 8;
        private static int JAVA_TWO_RELATIONS_ROW = 9;
        private static int POI_ONE_RELATION_ROW = 4;
        private static int POI_TWO_RELATIONS_ROW = 5;
        private static int POI_ONE_RELATION_ROW_XLS = 25;
        private static int POI_TWO_RELATIONS_ROW_XLS = 26;
        private static int COLUMN_25x25 = 1;
        private static int COLUMN_25x125 = 2;
        private static int COLUMN_25x250 = 3;
        private static int COLUMN_25x1250 = 4;
        private static int COLUMN_25x2500 = 5;

        private int getColumnToWrite(String benchmarkName) {
            if (benchmarkName.lastIndexOf("2500") != -1) {
                return COLUMN_25x2500;
            } else if (benchmarkName.lastIndexOf("1250") != -1) {
                return COLUMN_25x1250;
            } else if (benchmarkName.lastIndexOf("250") != -1) {
                return COLUMN_25x250;
            } else if (benchmarkName.lastIndexOf("125") != -1) {
                return COLUMN_25x125;
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
                if (benchmarkName.lastIndexOf("XLSX") != -1) {
                    if (benchmarkName.startsWith("One")) {
                        return POI_ONE_RELATION_ROW;
                    } else {
                        return POI_TWO_RELATIONS_ROW;
                    }
                } else {
                    if (benchmarkName.startsWith("One")) {
                        return POI_ONE_RELATION_ROW_XLS;
                    } else {
                        return POI_TWO_RELATIONS_ROW_XLS;
                    }
                }
            }
        }

        private int getRowsCount(BenchmarkUnit benchmarkUnit) {
            if (benchmarkUnit instanceof VerticalTableBenchmarkUnit) {
                return ((VerticalTableBenchmarkUnit) benchmarkUnit).getHeight();
            } else {
                return 1;
            }
        }

        private void saveStatistic(String benchmarkName, BenchmarkInfo benchmarkInfo, Sheet evaluationBenchmarkSheet) {
            int rowIndex = getRowToWrite(benchmarkName);
            int columnIndex = getColumnToWrite(benchmarkName);
            int formulaRowsCount = getRowsCount(benchmarkInfo.getUnit());
            evaluationBenchmarkSheet.getRow(rowIndex).getCell(columnIndex).setCellValue(
                    benchmarkInfo.avg() / formulaRowsCount);
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

    @Test
    public void testEvaluation() {
        long initialUsedMemory = BenchmarkWithMemory.getUsedMemorySizeBeforeTest();

        InputStream in = null;

        try {
            Workbook workbookXLSX = LiveExcelWorkbookFactory
                    .create((in = new FileInputStream(FORMULA_TEST_XLSX)), null);
            BenchmarkUnit[] bu = { new POIEvauation(workbookXLSX.getSheetAt(0)), new OneRelationJavaEvaluation(25, 25),
                    new POIEvauation(workbookXLSX.getSheetAt(1)), new TwoRelationJavaEvaluation(25, 25),
                    new POIEvauation(workbookXLSX.getSheetAt(2)), new OneRelationJavaEvaluation(25, 125),
                    new POIEvauation(workbookXLSX.getSheetAt(3)), new TwoRelationJavaEvaluation(25, 125),
                    new POIEvauation(workbookXLSX.getSheetAt(4)), new OneRelationJavaEvaluation(25, 250),
                    new POIEvauation(workbookXLSX.getSheetAt(5)), new TwoRelationJavaEvaluation(25, 250),
                    new POIEvauation(workbookXLSX.getSheetAt(6)), new OneRelationJavaEvaluation(25, 1250),
                    new POIEvauation(workbookXLSX.getSheetAt(7)), new TwoRelationJavaEvaluation(25, 1250),
                    new POIEvauation(workbookXLSX.getSheetAt(8)), new OneRelationJavaEvaluation(25, 2500),
                    new POIEvauation(workbookXLSX.getSheetAt(9)), new TwoRelationJavaEvaluation(25, 2500) };
            Map<String, BenchmarkInfo> res = new Benchmark(bu).measureAll(1000);

            new EvaluationStatisticsSaver().save(res, BenchmarkWithMemory.getUsedMemorySizeAfterTest() - initialUsedMemory);
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

//        try {
//            Workbook workbookXLS = LiveExcelWorkbookFactory.create((in = new FileInputStream(FORMULA_TEST_XLS)), null);
//            BenchmarkUnit[] bu = { new POIEvauation(workbookXLS.getSheetAt(0)),
//                    new POIEvauation(workbookXLS.getSheetAt(1)), new POIEvauation(workbookXLS.getSheetAt(2)),
//                    new POIEvauation(workbookXLS.getSheetAt(3)), new POIEvauation(workbookXLS.getSheetAt(4)),
//                    new POIEvauation(workbookXLS.getSheetAt(5)), new POIEvauation(workbookXLS.getSheetAt(6)),
//                    new POIEvauation(workbookXLS.getSheetAt(7)), new POIEvauation(workbookXLS.getSheetAt(8)),
//                    new POIEvauation(workbookXLS.getSheetAt(9)) };
//            Map<String, BenchmarkInfo> res = new Benchmark(bu).measureAll(1000);
//
//            new EvaluationStatisticsSaver().save(res, getUsedMemorySizeAfterTest() - initialUsedMemory);
//            assertTrue(true);
//        } catch (Exception e) {
//            e.printStackTrace();
//            assertTrue(false);
//        } finally {
//            try {
//                if (in != null)
//                    in.close();
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//        }
    }
}
