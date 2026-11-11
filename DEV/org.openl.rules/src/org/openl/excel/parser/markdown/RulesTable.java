package org.openl.excel.parser.markdown;

import java.util.Comparator;
import java.util.List;
import java.util.SequencedMap;
import java.util.stream.Collectors;

import org.apache.poi.ss.util.CellRangeAddress;
import org.jspecify.annotations.Nullable;

/**
 * A rules table of a Markdown module, before it is laid out on a sheet.
 *
 * @param header     the text of the first table cell, such as {@code Rules String Greeting(Integer hour)}
 * @param properties the table properties in the order they are written
 * @param body       the rows of the table body; a blank cell is {@code null}
 * @param merges     the merged ranges of the body, none of them overlapping
 */
record RulesTable(String header,
                  SequencedMap<String, String> properties,
                  List<List<@Nullable String>> body,
                  List<CellRangeAddress> merges) {

    /**
     * Returns the number of columns of the body: the length of its longest row.
     */
    int width() {
        return body.stream().mapToInt(List::size).max().orElse(0);
    }

    /**
     * Returns merged ranges in A1 notation, joined with commas, in reading order: the same ranges give the same text
     * in whatever order they are listed.
     */
    static String mergesText(List<CellRangeAddress> merges) {
        return inReadingOrder(merges).stream()
                .map(CellRangeAddress::formatAsString)
                .collect(Collectors.joining(", "));
    }

    /**
     * Returns merged ranges in reading order: by the row of their first cell, then by its column.
     */
    static List<CellRangeAddress> inReadingOrder(List<CellRangeAddress> merges) {
        return merges.stream()
                .sorted(Comparator.comparingInt(CellRangeAddress::getFirstRow)
                        .thenComparingInt(CellRangeAddress::getFirstColumn))
                .toList();
    }
}
