package org.openl.rules.xls.merge;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;

import org.apache.poi.openxml4j.util.ZipSecureFile;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.model.StylesTable;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.BeforeClass;
import org.junit.Test;

public class XlsSheetCopierTest {

    private static final Path FUNC_TEST = Paths.get("test-resources/functional-copy");
    private static final Path TARGET = Paths.get("target/test-merged");

    @BeforeClass
    public static void setUp() {
        ZipSecureFile.setMinInflateRatio(0.001);
    }

    @Test
    public void testFunctional() throws IOException {
        File[] files = FUNC_TEST.toFile().listFiles();
        assertNotNull(files);
        int counter = 0;
        boolean success = true;
        for (File file : files) {
            if (file.isDirectory()) {
                continue;
            }
            if (!file.getPath().endsWith(".xlsx") && file.getPath().equals(".xls")) {
                continue;
            }
            Path destCopyFile = TARGET.resolve(FUNC_TEST.relativize(Paths.get(file.getPath())));
            if (Files.exists(destCopyFile)) {
                Files.delete(destCopyFile);
            } else {
                Files.createDirectories(destCopyFile.getParent());
            }
            try (Workbook srcWorkbook = WorkbookFactory.create(file, null, true);
                    Workbook destWorkbook = WorkbookFactory.create(srcWorkbook instanceof XSSFWorkbook)) {
                if (srcWorkbook instanceof XSSFWorkbook) {
                    StylesTable srcStylesTable = ((XSSFWorkbook) srcWorkbook).getStylesSource();
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    srcStylesTable.writeTo(baos);
                    StylesTable destStylesTable = ((XSSFWorkbook) destWorkbook).getStylesSource();
                    destStylesTable.readFrom(new ByteArrayInputStream(baos.toByteArray()));
                    destStylesTable.setTheme(srcStylesTable.getTheme());
                }
                Iterator<Sheet> sheetIt = srcWorkbook.sheetIterator();
                while (sheetIt.hasNext()) {
                    Sheet srcSheet = sheetIt.next();
                    Sheet destSheet = destWorkbook.createSheet(srcSheet.getSheetName());
                    destWorkbook.setSheetOrder(destSheet.getSheetName(), srcWorkbook.getSheetIndex(srcSheet));
                    XlsSheetCopier.copy(srcWorkbook, srcSheet, destWorkbook, destSheet, null);
                }
                try (OutputStream out = Files.newOutputStream(destCopyFile)) {
                    destWorkbook.write(out);
                }
            }
            if (!XlsSheetsMatcherTest.matchWorkbooksTest(file.toPath(), destCopyFile)) {
                success = false;
            }
            counter++;
        }
        assertTrue(counter > 0);
        assertTrue(success);
    }

}
