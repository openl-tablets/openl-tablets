package org.openl.rules.dt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.openl.binding.IBindingContext;
import org.openl.rules.lang.xls.binding.XlsMetaInfo;
import org.openl.rules.lang.xls.types.meta.DtColumnsDefinitionMetaInfoReader;
import org.openl.rules.runtime.RulesEngineFactory;
import org.openl.rules.table.IGridTable;

/** Covers where a Conditions, an Actions or a Returns table holds the titles of the definitions it declares. */
class ADtColumnsDefinitionTableBoundNodeTest {

    @TempDir
    Path dir;

    @Test
    void findsTheTitlesInTheRowThatNamesThemInATableWithADefinitionInEachColumn() throws IOException {
        // The parts are rows named in the first column, as the Reference Guide writes the table.
        var titles = titlesOf(sheet -> {
            header(sheet, "Conditions AgeBand", 3);
            row(sheet, 1, "Inputs", "Integer age", "Integer age");
            row(sheet, 2, "Expression", "age >= minAge", "age <= maxAge");
            row(sheet, 3, "Parameter", "Integer minAge", "Integer maxAge");
            row(sheet, 4, "Title", "Age From", "Age To");
        });

        assertFalse(titles.isNormalOrientation(), "A table with a part in each row is read transposed");
        assertEquals(List.of("4:0 Title", "4:1 Age From", "4:2 Age To"), cellsOf(titles));
    }

    @Test
    void findsTheTitlesByTheirKeywordInATableWithAPartInEachColumn() throws IOException {
        var titles = titlesOf(sheet -> {
            header(sheet, "Returns Rates", 4);
            row(sheet, 1, "Title", "Inputs", "Expression", "Parameter");
            row(sheet, 2, "Base Rate", "Double rate", "rate * factor", "Double factor");
        });

        assertTrue(titles.isNormalOrientation());
        assertEquals(List.of("1:0 Title", "2:0 Base Rate"), cellsOf(titles));
    }

    @Test
    void findsNoTitlesInATableOfAnotherStructure() throws IOException {
        // Three parts in a row: neither way round does it hold the four parts of a definition.
        assertNull(titlesOf(sheet -> {
            header(sheet, "Conditions Broken", 3);
            row(sheet, 1, "Integer age", "age > min", "Integer min");
        }));
    }

    @Test
    void forgetsTheTitlesWithTheRestOfTheDebugInformation() throws IOException {
        var boundNode = boundNodeOf(sheet -> {
            header(sheet, "Returns Rates", 4);
            row(sheet, 1, "Double rate", "rate * factor", "Double factor", "Base Rate");
        });
        assertNotNull(boundNode.getTitles(), "The parts stand in the order the compiler expects when none is named");

        boundNode.removeDebugInformation(mock(IBindingContext.class));

        assertNull(boundNode.getTitles());
    }

    /** The titles the compiler finds in the one table of a sheet the content writes. */
    private @Nullable IGridTable titlesOf(Consumer<Sheet> content) throws IOException {
        return boundNodeOf(content).getTitles();
    }

    /** The one table of a sheet the content writes, as the compiler bound it. */
    private ADtColumnsDefinitionTableBoundNode boundNodeOf(Consumer<Sheet> content) throws IOException {
        var file = dir.resolve("Elements.xlsx");
        try (var workbook = new XSSFWorkbook(); var out = Files.newOutputStream(file)) {
            content.accept(workbook.createSheet("Elements"));
            workbook.write(out);
        }
        var compiled = new RulesEngineFactory<>(file.toUri().toURL()).getCompiledOpenClass();
        var node = ((XlsMetaInfo) compiled.getOpenClassWithErrors().getMetaInfo()).getXlsModuleNode()
                .getXlsTableSyntaxNodes()[0];
        return ((DtColumnsDefinitionMetaInfoReader) node.getMetaInfoReader()).getBoundNode();
    }

    /** The cells of a table: the row and the column of the sheet each stands in, and its text. */
    private static List<String> cellsOf(IGridTable table) {
        var cells = new ArrayList<String>();
        for (var row = 0; row < table.getHeight(); row++) {
            for (var column = 0; column < table.getWidth(); column++) {
                var cell = table.getCell(column, row);
                cells.add(cell.getAbsoluteRow() + ":" + cell.getAbsoluteColumn() + " " + cell.getStringValue());
            }
        }
        return cells;
    }

    /** The header of the table in the first row of the sheet, merged across its columns. */
    private static void header(Sheet sheet, String text, int width) {
        row(sheet, 0, text);
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, width - 1));
    }

    /** The values across a row of the sheet, from its first column. */
    private static void row(Sheet sheet, int index, String... values) {
        var row = sheet.createRow(index);
        for (var column = 0; column < values.length; column++) {
            row.createCell(column).setCellValue(values[column]);
        }
    }
}
