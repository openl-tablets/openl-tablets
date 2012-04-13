package org.openl.rules.test.liveexcel.benchmark;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.junit.Test;
import org.openl.rules.liveexcel.LiveExcelEvaluator;
import org.openl.rules.liveexcel.usermodel.ContextFactory;
import org.openl.rules.liveexcel.usermodel.LiveExcelWorkbookFactory;
import org.openl.rules.test.liveexcel.benchmark.FormulaEvaluationBenchmark.VerticalTableBenchmarkUnit;
import org.openl.util.benchmark.Benchmark;
import org.openl.util.benchmark.BenchmarkInfo;
import org.openl.util.benchmark.BenchmarkUnit;
import org.openl.util.benchmark.BenchmarkWithMemory;

public class LookupBenchmark {

    private static final String BENCHMARK_FILE = "./test/statistics/Benchmarks.xlsx";
    private static final String FILE_TO_PARSE_STARTING = "./test/resources/Lookup/";

    private static class DoubleLookup extends VerticalTableBenchmarkUnit {
        private LiveExcelEvaluator evaluator;
        private int size;
        private List<Object[]> evaluationArguments;

        public DoubleLookup(File lookupFile, int size) throws Exception {
            this.size = size;
            evaluator = new LiveExcelEvaluator(LiveExcelWorkbookFactory.create(new FileInputStream(lookupFile), null),
                    ContextFactory.getEvaluationContext(null));
            int offset = size / 5;
            evaluationArguments = new ArrayList<Object[]>();
            for (int i = 1; i < 6; i++) {
                for (int j = 0; j < 5; j++) {
                    evaluationArguments.add(new Object[] { 1 + j * offset, Integer.toString(i) });
                }
            }
        }

        @Override
        public int getHeight() {
            return size;
        }

        protected String getNameSpecial() {
            return "Double Lookup" + size;
        }

        public LiveExcelEvaluator getEvaluator() {
            return evaluator;
        }

        @Override
        protected void run() throws Exception {
            for (Object[] arguments : evaluationArguments) {
                evaluator.evaluateServiceModelUDF("lookup2", arguments);
            }
        }
    }

    private static class TripleLookup extends VerticalTableBenchmarkUnit {
        private LiveExcelEvaluator evaluator;
        private int size;
        private List<Object[]> evaluationArguments;

        public TripleLookup(File lookupFile, int size) throws Exception {
            this.size = size;
            evaluator = new LiveExcelEvaluator(LiveExcelWorkbookFactory.create(new FileInputStream(lookupFile), null),
                    ContextFactory.getEvaluationContext(null));
            int offset = size / 5;
            evaluationArguments = new ArrayList<Object[]>();
            for (int i = 1; i < 6; i++) {
                for (int j = 1; j < 6; j++) {
                    for (int k = 0; k < 5; k++) {
                        evaluationArguments
                                .add(new Object[] { 1 + k * offset, Integer.toString(i), Integer.toString(j) });
                    }
                }
            }
        }

        @Override
        public int getHeight() {
            return size;
        }

        protected String getNameSpecial() {
            return "Triple Lookup" + size;
        }

        public LiveExcelEvaluator getEvaluator() {
            return evaluator;
        }

        @Override
        protected void run() throws Exception {
            for (Object[] arguments : evaluationArguments) {
                evaluator.evaluateServiceModelUDF("lookup3", arguments);
            }
        }
    }

    private static class LookupStatisticsSaver {
        private static int MEMORY_USED_ROW = 6;
        private static int DOUBLE_LOOKUP_ROW = 3;
        private static int TRIPLE_LOOKUP_ROW = 4;
        private static int COLUMN_5 = 1;
        private static int COLUMN_25 = 2;
        private static int COLUMN_125 = 3;
        private static int COLUMN_625 = 4;
        private static int COLUMN_3125 = 5;

        private int getColumnToWrite(String benchmarkName) {
            if (benchmarkName.lastIndexOf("3125") != -1) {
                return COLUMN_3125;
            } else if (benchmarkName.lastIndexOf("625") != -1) {
                return COLUMN_625;
            } else if (benchmarkName.lastIndexOf("125") != -1) {
                return COLUMN_125;
            } else if (benchmarkName.lastIndexOf("25") != -1) {
                return COLUMN_25;
            } else {
                return COLUMN_5;
            }
        }

        private int getRowToWrite(String benchmarkName) {
            if (benchmarkName.startsWith("Double")) {
                return DOUBLE_LOOKUP_ROW;
            } else {
                return TRIPLE_LOOKUP_ROW;
            }
        }

        private int getEvalautionsCount(String benchmarkName) {
            if (benchmarkName.startsWith("Double")) {
                return 5;
            } else {
                return 25;
            }
        }

        private void saveStatistic(String benchmarkName, BenchmarkInfo benchmarkInfo, Sheet evaluationBenchmarkSheet) {
            int rowIndex = getRowToWrite(benchmarkName);
            int columnIndex = getColumnToWrite(benchmarkName);
            int evalautionsCount = getEvalautionsCount(benchmarkName);
            evaluationBenchmarkSheet.getRow(rowIndex).getCell(columnIndex).setCellValue(
                    benchmarkInfo.avg() / evalautionsCount);
        }

        public void save(Map<String, BenchmarkInfo> statistic, long memoryUsed) {
            OutputStream out = null;
            InputStream in = null;
            try {
                Workbook workbook = LiveExcelWorkbookFactory.create((in = new FileInputStream(BENCHMARK_FILE)), null);
                Sheet evaluationBenchmarkSheet = workbook.getSheetAt(3);
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
            List<BenchmarkUnit> buList = new ArrayList<BenchmarkUnit>();
            for (int i = 5; i < 3126; i *= 5) {
                buList.add(new DoubleLookup(new File(FILE_TO_PARSE_STARTING + i + ".xlsx"), i));
                buList.add(new TripleLookup(new File(FILE_TO_PARSE_STARTING + i + ".xlsx"), i));
            }
            Map<String, BenchmarkInfo> res = new Benchmark((BenchmarkUnit[]) buList.toArray(new BenchmarkUnit[buList
                    .size()])).measureAll(1000);

            new LookupStatisticsSaver().save(res, BenchmarkWithMemory.getUsedMemorySizeAfterTest() - initialUsedMemory);
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
