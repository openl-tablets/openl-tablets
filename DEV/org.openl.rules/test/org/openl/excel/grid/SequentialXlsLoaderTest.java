package org.openl.excel.grid;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.openl.rules.lang.xls.syntax.XlsModuleSyntaxNode;
import org.openl.source.impl.PathSourceCodeModule;
import org.openl.syntax.exception.SyntaxNodeException;

class SequentialXlsLoaderTest {

    @TempDir
    private Path dir;

    @Test
    void getParentAndMergePaths() {
        assertEquals("/f1/f2/m2", SequentialXlsLoader.getParentAndMergePaths("/f1/f2/m1", "m2"));
        assertEquals("/f1/f2/m2", SequentialXlsLoader.getParentAndMergePaths("/f1/f2/", "m2"));
        assertEquals("C:/f1/f2/m2", SequentialXlsLoader.getParentAndMergePaths("C:\\f1\\f2\\", "m2"));
        assertEquals("file://f1/f2/m2", SequentialXlsLoader.getParentAndMergePaths("file://f1/f2/m1", "m2"));
        assertEquals("file://C:/f1/f2/m2",
                SequentialXlsLoader.getParentAndMergePaths("file://C:\\f1\\f2\\", "m2"));

        assertEquals("/f1/f2/m2", SequentialXlsLoader.getParentAndMergePaths("/f1/./././f2/m1", "m2"));
        assertEquals("/f1/f2/m2", SequentialXlsLoader.getParentAndMergePaths("/f1/f2/", "./m2"));
        assertEquals("/m2", SequentialXlsLoader.getParentAndMergePaths("/f1/f2/m1", "../../m2"));
        assertEquals("/f2/m2", SequentialXlsLoader.getParentAndMergePaths("/f1/../f2/", "m2"));
    }

    @Test
    void anIncludeInAParentFolderIsReadFromADeploymentArchive() throws IOException {
        var archive = dir.resolve("deployment.zip");
        try (var zip = FileSystems.newFileSystem(archive, Map.of("create", "true"))) {
            workbook(zip.getPath("rules/main/Main.xlsx"), "Environment", "include", "../include/My Data.xlsx");
            workbook(zip.getPath("rules/include/My Data.xlsx"), "Datatype Car", "String", "model");
        }

        try (var zip = FileSystems.newFileSystem(archive)) {
            var parsed = new SequentialXlsLoader().parse(new PathSourceCodeModule(zip.getPath("rules/main/Main.xlsx")));

            assertArrayEquals(new SyntaxNodeException[0], parsed.getErrors());
            var tables = Arrays.stream(((XlsModuleSyntaxNode) parsed.getTopNode()).getXlsTableSyntaxNodes())
                    .map(table -> table.getGridTable().getCell(0, 0).getStringValue())
                    .sorted()
                    .toList();
            assertEquals(List.of("Datatype Car", "Environment"), tables);
        }
    }

    /** Writes a workbook holding one table: its header, then a row of two cells. */
    private static void workbook(Path file, String header, String first, String second) throws IOException {
        Files.createDirectories(file.getParent());
        try (var workbook = new XSSFWorkbook(); var out = Files.newOutputStream(file)) {
            var sheet = workbook.createSheet("Main");
            sheet.createRow(0).createCell(0).setCellValue(header);
            var row = sheet.createRow(1);
            row.createCell(0).setCellValue(first);
            row.createCell(1).setCellValue(second);
            workbook.write(out);
        }
    }
}
