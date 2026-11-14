package org.openl.rules.lang.xls.load;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipFile;

import org.apache.poi.openxml4j.util.ZipArchiveFakeEntry;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.openl.source.impl.PathSourceCodeModule;

/**
 * A workbook with a part larger than POI reads into memory by default is still read for editing.
 *
 * @author Yury Molchan
 */
class LargeWorkbookLoadTest {

    /** The largest part POI reads into memory, lowered so that a small workbook goes over it. */
    private static final int MAX_PART_BYTES = 2_000_000;

    @TempDir
    Path folder;

    private int maxEntrySize;

    @BeforeEach
    void lowerMaxPartSize() {
        maxEntrySize = ZipArchiveFakeEntry.getMaxEntrySize();
        ZipArchiveFakeEntry.setMaxEntrySize(MAX_PART_BYTES);
    }

    @AfterEach
    void restoreMaxPartSize() {
        ZipArchiveFakeEntry.setMaxEntrySize(maxEntrySize);
    }

    @Test
    void readsPartLargerThanAllowedInMemory() throws IOException {
        var file = folder.resolve("Large.xlsx");
        // A few cells with long text make a large part, the text of the workbook, out of little memory.
        var text = "x".repeat(1_200);
        var rows = 2_500;
        try (var workbook = new XSSFWorkbook(); var out = Files.newOutputStream(file)) {
            var sheet = workbook.createSheet("Rules");
            for (var row = 0; row < rows; row++) {
                sheet.createRow(row).createCell(0).setCellValue(row + text);
            }
            workbook.write(out);
        }
        try (var zip = new ZipFile(file.toFile())) {
            assertTrue(zip.getEntry("xl/sharedStrings.xml").getSize() > MAX_PART_BYTES);
        }

        try (var workbook = WorkbookLoadUtils.loadWorkbook(new PathSourceCodeModule(file))) {
            assertEquals(rows - 1, workbook.getSheet("Rules").getLastRowNum());
        }
    }
}
