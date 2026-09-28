package org.openl.rules.lang.xls;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/** Workbooks on disk for the tests that read one back. */
public final class TestWorkbooks {

    /** What a cell of a workbook written by {@link #writeOneCell} says. */
    public static final String CELL = "read from the file";

    private TestWorkbooks() {
    }

    /**
     * Writes a workbook of one sheet and one cell into the given folder.
     *
     * @param folder where to write it
     * @return the file it was written to
     */
    public static Path writeOneCell(Path folder) throws IOException {
        var file = folder.resolve("Test.xlsx");
        try (var workbook = new XSSFWorkbook(); var out = Files.newOutputStream(file)) {
            workbook.createSheet("Sheet1").createRow(0).createCell(0).setCellValue(CELL);
            workbook.write(out);
        }
        return file;
    }
}
