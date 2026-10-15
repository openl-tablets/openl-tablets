package org.openl.excel.parser.markdown;

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
     * Returns merged ranges in A1 notation, joined with commas, the way the front matter lists them.
     */
    static String mergesText(List<CellRangeAddress> merges) {
        return merges.stream().map(CellRangeAddress::formatAsString).collect(Collectors.joining(", "));
    }
}
