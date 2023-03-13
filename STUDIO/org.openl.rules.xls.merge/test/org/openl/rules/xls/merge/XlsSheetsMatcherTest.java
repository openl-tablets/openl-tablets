package org.openl.rules.xls.merge;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;

import org.apache.poi.openxml4j.util.ZipSecureFile;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.helpers.MessageFormatter;

public class XlsSheetsMatcherTest {

    private static final Path TEST_RES = Paths.get("test-resources");
    private static final Path TEST_CL = Paths.get("target/test-classes");
    private static final Logger LOG = LoggerFactory.getLogger("Excel Sheet Matcher Test");

    @BeforeClass
    public static void setUp() {
        ZipSecureFile.setMinInflateRatio(0.001);
    }

    @Test
    public void testBaseWorkbook_shouldAlwaysMatchItself() throws IOException {
        assertTrue(matchWorkbooksTest(TEST_RES.resolve("BaseWorkbook.xlsx"), TEST_CL.resolve("BaseWorkbook.xlsx")));
    }

    public static boolean matchWorkbooksTest(Path pathToWorkbook1, Path pathToWorkbook2) throws IOException {
        boolean failed = false;
        try (Workbook workbook1 = WorkbookFactory.create(pathToWorkbook1.toFile(), null, true);
             Workbook workbook2 = WorkbookFactory.create(pathToWorkbook2.toFile(), null, true)) {
            LOG.info("Testing \u001B[2;36m{}\u001B[0m and \u001B[2;36m{}\u001B[0m", pathToWorkbook1, pathToWorkbook2);
            Iterator<Sheet> sheetIt1 = workbook1.sheetIterator();
            while (sheetIt1.hasNext()) {
                Sheet sheet1 = sheetIt1.next();
                Sheet sheet2 = workbook2.getSheet(sheet1.getSheetName());
                if (sheet2 == null) {
                    failed = true;
                    error(sheet1.getSheetName(), "Cannot find {} in {}", sheet1.getSheetName(), pathToWorkbook2);
                } else if (XlsSheetsMatcher.hasChanges(workbook1, sheet1, workbook2, sheet2)) {
                    failed = true;
                    error(sheet1.getSheetName(), "Sheets are not the same");
                } else {
                    ok(sheet1.getSheetName());
                }
            }
        }

        return !failed;
    }

    @Test
    public void testBaseWorkbookAndChanged_shouldAlwaysChanged() throws IOException {
        assertTrue(notMatchWorkbooksTest(TEST_RES.resolve("BaseWorkbook.xlsx"), TEST_RES.resolve("Changed Workbook.xlsx")));
    }

    private boolean notMatchWorkbooksTest(Path pathToWorkbook1, Path pathToWorkbook2) throws IOException {
        boolean failed = false;
        try (Workbook workbook1 = WorkbookFactory.create(pathToWorkbook1.toFile(), null, true);
             Workbook workbook2 = WorkbookFactory.create(pathToWorkbook2.toFile(), null, true)) {
            LOG.info("Testing \u001B[2;36m{}\u001B[0m and \u001B[2;36m{}\u001B[0m", pathToWorkbook1, pathToWorkbook2);
            Iterator<Sheet> sheetIt1 = workbook1.sheetIterator();
            while (sheetIt1.hasNext()) {
                Sheet sheet1 = sheetIt1.next();
                Sheet sheet2 = workbook2.getSheet(sheet1.getSheetName());
                if (sheet2 == null) {
                    failed = true;
                    error(sheet1.getSheetName(), "Cannot find {} in {}", sheet1.getSheetName(), pathToWorkbook2);
                } else if (!XlsSheetsMatcher.hasChanges(workbook1, sheet1, workbook2, sheet2)) {
                    failed = true;
                    error(sheet1.getSheetName(), "Sheets are the same");
                } else {
                    ok(sheet1.getSheetName());
                }
            }
        }

        return !failed;
    }

    private static void ok(String sourceFile) {
        LOG.info("\u001B[1;32mSUCCESS\u001B[0m - in [\u001B[2;36m{}\u001B[0m] sheet", sourceFile);
    }

    private static void error(String sourceFile, String msg, Object... args) {
        LOG.error("\u001B[1;31mFAILURE\u001B[0m - in [\u001B[2;36m{}\u001B[0m] sheet\n        {}",
            sourceFile,
            MessageFormatter.arrayFormat(msg, args).getMessage());
    }

}
