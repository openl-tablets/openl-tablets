package org.openl.rules.lang.xls;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import org.openl.rules.lang.xls.binding.XlsMetaInfo;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.runtime.RulesEngineFactory;

/** Workbooks on disk for the tests that read one back. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class TestWorkbooks {

    /** What a cell of a workbook written by {@link #writeOneCell} says. */
    public static final String CELL = "read from the file";

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

    /**
     * Compiles a workbook of one sheet into the given folder and gives its first table.
     *
     * <p>The table is given as the compiler bound it, with its errors when it does not compile.
     *
     * @param folder  where to write the workbook
     * @param content writes the sheet
     * @return the first table of the sheet
     */
    public static TableSyntaxNode compileFirstTable(Path folder, Consumer<Sheet> content) throws IOException {
        var file = folder.resolve("Rules.xlsx");
        try (var workbook = new XSSFWorkbook(); var out = Files.newOutputStream(file)) {
            content.accept(workbook.createSheet("Rules"));
            workbook.write(out);
        }
        var compiled = new RulesEngineFactory<>(file.toUri().toURL()).getCompiledOpenClass();
        return ((XlsMetaInfo) compiled.getOpenClassWithErrors().getMetaInfo()).getXlsModuleNode()
                .getXlsTableSyntaxNodes()[0];
    }
}
