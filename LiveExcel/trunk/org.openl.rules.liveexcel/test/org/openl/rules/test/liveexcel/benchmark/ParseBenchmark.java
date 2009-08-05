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
import org.openl.rules.liveexcel.usermodel.LiveExcelWorkbookFactory;
import org.openl.rules.test.liveexcel.benchmark.FormulaEvaluationBenchmark.VerticalTableBenchmarkUnit;
import org.openl.util.benchmark.BenchmarkInfo;
import org.openl.util.benchmark.BenchmarkInfoWithMemory;
import org.openl.util.benchmark.BenchmarkUnit;
import org.openl.util.benchmark.BenchmarkWithMemory;

//import static org.junit.Assert.*;

public class ParseBenchmark {

    private static final String BENCHMARK_FILE = "./test/statistics/Benchmarks.xlsx";
    private static final String FILE_TO_PARSE_STARTING = "./test/resources/ParsingTest/10x";

    private static class ParsingBenchmark extends VerticalTableBenchmarkUnit {
        private File fileToParse;
        private int height;

        public ParsingBenchmark(File fileToParse, int height) {
            this.fileToParse = fileToParse;
            this.height = height;
        }

        protected String getNameSpecial() {
            return fileToParse.getName();
        }

        @Override
        protected void run() throws Exception {
            LiveExcelWorkbookFactory.create(new FileInputStream(fileToParse), null);
        }

        public int getHeight() {
            return height;
        }

        public long getFileSize() {
            return fileToParse.length();
        }
    }

    private static class ParsingStatisticsSaver {
        private static int PARSING_TIME_OFFSET = 0;
        private static int USED_MEMORY_OFFSET = 1;
        private static int FILE_SIZE_OFFSET = 2;
        private static int XLSX_STARTING_ROW = 11;
        private static int XLS_STARTING_ROW = 3;
        private static int COLUMN_10x10 = 1;
        private static int COLUMN_10x50 = 2;
        private static int COLUMN_10x100 = 3;
        private static int COLUMN_10x500 = 4;
        private static int COLUMN_10x1000 = 5;

        private int getColumnToWrite(String benchmarkName) {
            if (benchmarkName.lastIndexOf("1000") != -1) {
                return COLUMN_10x1000;
            } else if (benchmarkName.lastIndexOf("500") != -1) {
                return COLUMN_10x500;
            } else if (benchmarkName.lastIndexOf("100") != -1) {
                return COLUMN_10x100;
            } else if (benchmarkName.lastIndexOf("50") != -1) {
                return COLUMN_10x50;
            } else {
                return COLUMN_10x10;
            }
        }

        private int getRowToWrite(String benchmarkName, int offset) {
            if (benchmarkName.endsWith("xlsx")) {
                return XLSX_STARTING_ROW + offset;
            } else {
                return XLS_STARTING_ROW + offset;
            }
        }

        private void saveParsingTime(BenchmarkInfo benchmarkInfo, Sheet evaluationBenchmarkSheet) {
            ParsingBenchmark benchmark = (ParsingBenchmark) benchmarkInfo.getUnit();
            int rowIndex = getRowToWrite(benchmarkInfo.getUnit().getName(), PARSING_TIME_OFFSET);
            int columnIndex = getColumnToWrite(benchmarkInfo.getUnit().getName());
            evaluationBenchmarkSheet.getRow(rowIndex).getCell(columnIndex).setCellValue(
                    benchmarkInfo.avg() / benchmark.getHeight());
        }

        private void saveMemoryUsed(BenchmarkInfoWithMemory benchmarkInfo, Sheet evaluationBenchmarkSheet) {
            ParsingBenchmark benchmark = (ParsingBenchmark) benchmarkInfo.getUnit();
            int rowIndex = getRowToWrite(benchmarkInfo.getUnit().getName(), USED_MEMORY_OFFSET);
            int columnIndex = getColumnToWrite(benchmarkInfo.getUnit().getName());
            evaluationBenchmarkSheet.getRow(rowIndex).getCell(columnIndex).setCellValue(
                    benchmarkInfo.getMemoryUsed() / benchmark.getHeight());
        }

        private void saveFileSize(ParsingBenchmark benchmark, Sheet evaluationBenchmarkSheet) {
            int rowIndex = getRowToWrite(benchmark.getName(), FILE_SIZE_OFFSET);
            int columnIndex = getColumnToWrite(benchmark.getName());
            evaluationBenchmarkSheet.getRow(rowIndex).getCell(columnIndex).setCellValue(
                    benchmark.getFileSize() / benchmark.getHeight());
        }

        private void saveStatistic(BenchmarkInfo benchmarkInfo, Sheet evaluationBenchmarkSheet) {
            if (benchmarkInfo.getUnit() instanceof ParsingBenchmark) {
                ParsingBenchmark benchmark = (ParsingBenchmark) benchmarkInfo.getUnit();
                saveParsingTime(benchmarkInfo, evaluationBenchmarkSheet);
                saveMemoryUsed((BenchmarkInfoWithMemory) benchmarkInfo, evaluationBenchmarkSheet);
                saveFileSize(benchmark, evaluationBenchmarkSheet);
            }
        }

        public void save(Map<String, BenchmarkInfo> statistic) {
            OutputStream out = null;
            InputStream in = null;
            try {
                Workbook workbook = LiveExcelWorkbookFactory.create((in = new FileInputStream(BENCHMARK_FILE)), null);
                Sheet parsingBenchmarkSheet = workbook.getSheetAt(1);
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
        try {
            List<BenchmarkUnit> buList = new ArrayList<BenchmarkUnit>();
            int multiplier = 2;
            for (int i = 10; i < 1001; i *= multiplier) {
                buList.add(new ParsingBenchmark(new File(FILE_TO_PARSE_STARTING + i + ".xlsx"), i));
                buList.add(new ParsingBenchmark(new File(FILE_TO_PARSE_STARTING + i + ".xls"), i));
                if (multiplier == 2) {
                    multiplier = 5;
                } else {
                    multiplier = 2;
                }
            }

            Map<String, BenchmarkInfo> res = new BenchmarkWithMemory((BenchmarkUnit[]) buList
                    .toArray(new BenchmarkUnit[buList.size()])).measureAll(1000);
            new ParsingStatisticsSaver().save(res);
            assertTrue(true);
        } catch (Exception e) {
            e.printStackTrace();
            assertTrue(false);
        }
    }
}
