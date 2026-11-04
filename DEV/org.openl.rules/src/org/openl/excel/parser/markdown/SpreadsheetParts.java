package org.openl.excel.parser.markdown;

import java.util.BitSet;
import java.util.List;
import java.util.Objects;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.poi.ss.util.CellRangeAddress;
import org.commonmark.node.Paragraph;
import org.commonmark.parser.Parser;
import org.jspecify.annotations.Nullable;

import org.openl.rules.lang.xls.IXlsTableNames;

/**
 * The parts a Spreadsheet table of a Markdown file is written in.
 *
 * <p>A Spreadsheet groups its steps under captions: a row whose first cell holds the caption and is merged across the
 * table. A file writes such a table as GFM tables one after another, a caption between each two as a paragraph of its
 * own, so that the file needs no merged range for them. Every part after the first repeats the first row of the table,
 * the names of its columns, as GFM wants a table to start with them; the table holds that row once.
 *
 * <p>A caption is a line of text that reads back as a paragraph of the same text: it does not start a heading, a list,
 * a quote, a code block or a table row. A caption of any other text, and a caption of a table of one column, is
 * written as a merged range.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class SpreadsheetParts {

    private static final Parser PARSER = Parser.builder().build();

    /**
     * Tells whether a table header is the header of a Spreadsheet, such as {@code Spreadsheet Double total()}.
     */
    static boolean isSpreadsheet(String header) {
        var keyword = header.strip().split("[ \n\r]", 2)[0];
        return IXlsTableNames.SPREADSHEET_TABLE.equals(keyword) || IXlsTableNames.SPREADSHEET_TABLE2.equals(keyword);
    }

    /**
     * Tells whether a text is written as a caption: a line that reads back as a paragraph of the same text.
     */
    static boolean isCaption(@Nullable String text) {
        if (text == null || text.isBlank() || !text.equals(text.strip()) || text.indexOf('\n') >= 0
                || text.indexOf('\r') >= 0 || text.startsWith("|")) {
            return false;
        }
        var document = PARSER.parse(text);
        return document.getFirstChild() instanceof Paragraph && document.getFirstChild().getNext() == null;
    }

    /**
     * Returns the text of a caption written in a block of lines: its lines joined with spaces.
     */
    static String captionOf(List<String> lines, TableFile.Block block) {
        var text = new StringBuilder();
        for (var line = block.firstLine(); line <= block.lastLine(); line++) {
            text.append(text.isEmpty() ? "" : " ").append(lines.get(line).strip());
        }
        return text.toString();
    }

    /**
     * Returns the merged range of a caption in a row of a table of a width.
     */
    static CellRangeAddress rangeOf(int row, int width) {
        return new CellRangeAddress(row, row, 0, width - 1);
    }

    /**
     * Returns the rows of a table written as captions: the rows of a Spreadsheet, the first row aside, that hold a
     * caption in their first cell alone and are merged across the table.
     */
    static BitSet captionRows(RulesTable table) {
        var rows = new BitSet();
        var width = table.width();
        if (width < 2 || !isSpreadsheet(table.header())) {
            return rows;
        }
        for (var range : table.merges()) {
            var row = range.getFirstRow();
            if (row > 0 && range.equals(rangeOf(row, width)) && isCaptionRow(table.body().get(row))) {
                rows.set(row);
            }
        }
        return rows;
    }

    private static boolean isCaptionRow(List<@Nullable String> cells) {
        return isCaption(cells.getFirst()) && cells.stream().skip(1).allMatch(Objects::isNull);
    }

    /**
     * Returns the merged ranges of a table but those of the rows written as captions.
     */
    static List<CellRangeAddress> mergesBesides(List<CellRangeAddress> merges, BitSet captionRows) {
        return merges.stream()
                .filter(range -> range.getFirstRow() != range.getLastRow() || !captionRows.get(range.getFirstRow()))
                .toList();
    }
}
