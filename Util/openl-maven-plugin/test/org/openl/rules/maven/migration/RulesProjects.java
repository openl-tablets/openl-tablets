package org.openl.rules.maven.migration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;
import java.util.stream.StreamSupport;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import org.openl.message.OpenLMessage;
import org.openl.message.Severity;
import org.openl.rules.project.instantiation.SimpleProjectEngineFactory;

/**
 * Writes, compiles and reads the one-workbook projects the tests of the {@code rules.*} migrators rewrite.
 */
final class RulesProjects {

    private static final String RULES_XML = """
            <project>
                <name>migrate</name>
                <modules>
                    <module>
                        <name>Main</name>
                        <rules-root path="Main.xlsx"/>
                    </module>
                </modules>
            </project>
            """;

    private RulesProjects() {
    }

    static MigrationContext context(Path folder) {
        return new MigrationContext(null, new Compilation(() -> compile(folder)), null, Set.of());
    }

    /**
     * Builds the rules of the project to call them.
     */
    static SimpleProjectEngineFactory<?> engine(Path folder) {
        return new SimpleProjectEngineFactory.SimpleProjectEngineFactoryBuilder<>()
                .setProject(folder.toString())
                .build();
    }

    static Collection<OpenLMessage> compile(Path folder) {
        try {
            return new SimpleProjectEngineFactory.SimpleProjectEngineFactoryBuilder<>()
                    .setProject(folder.toString())
                    .setExecutionMode(false)
                    .build()
                    .getCompiledOpenClass()
                    .getAllMessages();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    static List<String> errors(Collection<OpenLMessage> messages) {
        return messages.stream().filter(m -> m.getSeverity() == Severity.ERROR).map(OpenLMessage::getSummary).toList();
    }

    static String[] row(String... cells) {
        return cells;
    }

    /**
     * Writes the tables one under another, with an empty row between them, and merges the header of a table over
     * all its columns.
     */
    static Path writeProject(Path folder, List<List<String[]>> tables) throws IOException {
        return writeProject(folder, new XSSFWorkbook(), tables);
    }

    /**
     * Writes the tables with the workbook, which decides how the texts of the cells are stored.
     */
    static Path writeProject(Path folder, Workbook book, List<List<String[]>> tables) throws IOException {
        Files.writeString(folder.resolve("rules.xml"), RULES_XML);
        var file = folder.resolve("Main.xlsx");
        try (var workbook = book; var out = Files.newOutputStream(file)) {
            var sheet = workbook.createSheet("Main");
            var r = 0;
            for (var table : tables) {
                var width = table.stream().mapToInt(cells -> cells.length).max().orElse(1);
                if (width > 1) {
                    sheet.addMergedRegion(new CellRangeAddress(r, r, 0, width - 1));
                }
                for (var cells : table) {
                    var row = sheet.createRow(r++);
                    for (var c = 0; c < cells.length; c++) {
                        row.createCell(c).setCellValue(cells[c]);
                    }
                }
                r++;
            }
            workbook.write(out);
        }
        return file;
    }

    /**
     * Reads the rows of the first sheet, joining the texts of the cells of a row with {@code |}.
     */
    static List<String> read(Path file) throws IOException {
        try (var workbook = WorkbookFactory.create(file.toFile(), null, true)) {
            var sheet = workbook.getSheetAt(0);
            return IntStream.rangeClosed(0, sheet.getLastRowNum())
                    .mapToObj(sheet::getRow)
                    .map(row -> row == null ? "" : StreamSupport.stream(row.spliterator(), false)
                            .map(Cell::getStringCellValue)
                            .reduce((a, b) -> a + "|" + b)
                            .orElse(""))
                    .toList();
        }
    }
}
