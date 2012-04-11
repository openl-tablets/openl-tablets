package org.openl.rules.test.liveexcel.benchmark;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.junit.Test;
import org.openl.rules.liveexcel.LiveExcelEvaluator;
import org.openl.rules.liveexcel.usermodel.ContextFactory;
import org.openl.rules.liveexcel.usermodel.LiveExcelWorkbookFactory;
import org.openl.rules.test.liveexcel.benchmark.FormulaEvaluationBenchmark.VerticalTableBenchmarkUnit;
import org.openl.util.benchmark.BenchmarkInfo;
import org.openl.util.benchmark.BenchmarkInfoWithMemory;
import org.openl.util.benchmark.BenchmarkUnit;
import org.openl.util.benchmark.BenchmarkWithMemory;

public class UDFsBenchmark {
    private static final String BENCHMARK_FILE = "./test/statistics/Benchmarks.xlsx";
    private static final String FILE_TO_PARSE_STARTING = "./test/resources/UDFTest/";

    private static class UDFEvaluation extends VerticalTableBenchmarkUnit {
        private LiveExcelEvaluator evaluator;
        private int size;

        public UDFEvaluation(LiveExcelEvaluator evaluator, int size) {
            this.evaluator = evaluator;
            this.size = size;
        }

        @Override
        public int getHeight() {
            return size;
        }

        protected String getNameSpecial() {
            return "UDFEvaluation" + size;
        }

        @Override
        protected void run() throws Exception {
            for (int i = 0; i < size; i++) {
                evaluator.evaluateServiceModelUDF("func" + i, new Object[] { 1, 2 });
            }
        }
    }

    private static class UDFParsing extends VerticalTableBenchmarkUnit {
        private File fileToParse;
        private LiveExcelEvaluator evaluator;
        private int size;

        public UDFParsing(File fileToParse, int size) {
            this.fileToParse = fileToParse;
            this.size = size;
        }

        @Override
        public int getHeight() {
            return size;
        }

        protected String getNameSpecial() {
            return "UDFParsing" + size;
        }

        public LiveExcelEvaluator getEvaluator() {
            return evaluator;
        }

        @Override
        protected void run() throws Exception {
            evaluator = new LiveExcelEvaluator(LiveExcelWorkbookFactory.create(new FileInputStream(fileToParse), null),
                    ContextFactory.getEvaluationContext(null));
        }
    }

    private static class UDFStatisticsSaver {
        private static int EVALUATION_TIME_OFFSET = 0;
        private static int PARSING_TIME_OFFSET = 1;
        private static int USED_MEMORY_OFFSET = 2;
        private static int STARTING_ROW = 3;
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

        private void saveEvaluationTime(BenchmarkInfo benchmarkInfo, Sheet evaluationBenchmarkSheet) {
            UDFEvaluation benchmark = (UDFEvaluation) benchmarkInfo.getUnit();
            int rowIndex = STARTING_ROW + EVALUATION_TIME_OFFSET;
            int columnIndex = getColumnToWrite(benchmarkInfo.getUnit().getName());
            evaluationBenchmarkSheet.getRow(rowIndex).getCell(columnIndex).setCellValue(
                    benchmarkInfo.avg() / benchmark.getHeight());
        }

        private void saveParsingTime(BenchmarkInfo benchmarkInfo, Sheet evaluationBenchmarkSheet) {
            UDFParsing benchmark = (UDFParsing) benchmarkInfo.getUnit();
            int rowIndex = STARTING_ROW + PARSING_TIME_OFFSET;
            int columnIndex = getColumnToWrite(benchmarkInfo.getUnit().getName());
            evaluationBenchmarkSheet.getRow(rowIndex).getCell(columnIndex).setCellValue(
                    benchmarkInfo.avg() / benchmark.getHeight());
        }

        private void saveMemoryUsed(BenchmarkInfoWithMemory benchmarkInfo, Sheet evaluationBenchmarkSheet) {
            UDFParsing benchmark = (UDFParsing) benchmarkInfo.getUnit();
            int rowIndex = STARTING_ROW + USED_MEMORY_OFFSET;
            int columnIndex = getColumnToWrite(benchmark.getName());
            evaluationBenchmarkSheet.getRow(rowIndex).getCell(columnIndex).setCellValue(
                    benchmarkInfo.getMemoryUsed() / benchmark.getHeight());
        }

        private void saveStatistic(BenchmarkInfo benchmarkInfo, Sheet evaluationBenchmarkSheet) {
            if (benchmarkInfo.getUnit() instanceof UDFEvaluation) {
                saveEvaluationTime(benchmarkInfo, evaluationBenchmarkSheet);
            } else if (benchmarkInfo instanceof BenchmarkInfoWithMemory) {
                saveParsingTime(benchmarkInfo, evaluationBenchmarkSheet);
                saveMemoryUsed((BenchmarkInfoWithMemory) benchmarkInfo, evaluationBenchmarkSheet);
            }
        }

        public void save(Map<String, BenchmarkInfo> statistic) {
            OutputStream out = null;
            InputStream in = null;
            try {
                Workbook workbook = LiveExcelWorkbookFactory.create((in = new FileInputStream(BENCHMARK_FILE)), null);
                Sheet parsingBenchmarkSheet = workbook.getSheetAt(2);
                for (String benchmarkName : statistic.keySet()) {
                    saveStatistic(statistic.get(benchmarkName), parsingBenchmarkSheet);
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
    public void testParsing() {
        Map<String, BenchmarkInfo> benchmarkResults = new HashMap<String, BenchmarkInfo>();
        try {
            Map<String, BenchmarkInfo> parsingResults = parse();
            Map<String, BenchmarkInfo> evaluationResults = evaluate(parsingResults);
            benchmarkResults.putAll(parsingResults);
            benchmarkResults.putAll(evaluationResults);
            new UDFStatisticsSaver().save(benchmarkResults);
            assertTrue(true);
        } catch (Exception e) {
            e.printStackTrace();
            assertTrue(false);
        }
    }

    private Map<String, BenchmarkInfo> parse() throws Exception {
        List<BenchmarkUnit> buList = new ArrayList<BenchmarkUnit>();
        for (int i = 5; i < 3126; i *= 5) {
            buList.add(new UDFParsing(new File(FILE_TO_PARSE_STARTING + i + ".xlsx"), i));
        }
        return new BenchmarkWithMemory((BenchmarkUnit[]) buList.toArray(new BenchmarkUnit[buList.size()]))
                .measureAll(1000);
    }

    private Map<String, BenchmarkInfo> evaluate(Map<String, BenchmarkInfo> parsingResults) throws Exception {
        List<BenchmarkUnit> buList = new ArrayList<BenchmarkUnit>();
        for (BenchmarkInfo benchmarkInfo : parsingResults.values()) {
            UDFParsing parsingUnit = (UDFParsing) benchmarkInfo.getUnit();
            buList.add(new UDFEvaluation(parsingUnit.getEvaluator(), parsingUnit.getHeight()));
        }
        return new BenchmarkWithMemory((BenchmarkUnit[]) buList.toArray(new BenchmarkUnit[buList.size()]))
                .measureAll(1000);
    }
}
