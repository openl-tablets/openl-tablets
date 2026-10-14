package org.openl.excel.parser.markdown;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.jspecify.annotations.Nullable;

import org.openl.excel.parser.ExcelParseException;
import org.openl.excel.parser.ExcelReader;
import org.openl.excel.parser.SheetDescriptor;
import org.openl.excel.parser.TableStyles;
import org.openl.rules.table.IGridRegion;
import org.openl.util.FileUtils;

/**
 * Reads a Markdown file of rules as a workbook of one sheet.
 *
 * <p>The sheet holds the table of the file, laid out the way Excel would hold it, and is named after the folder the
 * file is in: a module folder gives its tables a sheet each, and a sub-folder groups the tables in it. A file without
 * a table gives an empty sheet. The file keeps no formatting, so all cells have the default look, and every value is
 * text.
 *
 * <p>The whole file is read when the reader is created.
 *
 * @see MarkdownTableParser for the format
 */
public final class MarkdownReader implements ExcelReader {

    private static final char BYTE_ORDER_MARK = '﻿';

    private final MarkdownSheet sheet;

    /**
     * Reads a file from a stream. The stream is not closed.
     *
     * @param sourceName the path or URI of the file; the folder it names is the name of the sheet
     * @throws MarkdownFormatException when the file breaks the format
     */
    public MarkdownReader(InputStream stream, String sourceName) {
        var text = read(stream);
        var withoutMark = !text.isEmpty() && text.charAt(0) == BYTE_ORDER_MARK ? text.substring(1) : text;
        var cells = MarkdownTableParser.parse(withoutMark, FileUtils.getName(sourceName))
                .map(SheetLayout::layOut)
                .orElseGet(() -> new Object[0][]);
        sheet = new MarkdownSheet(sheetNameOf(sourceName), cells);
    }

    /**
     * Returns the sheet of a file that could not be read: named as its sheet would be, and holding no cells.
     *
     * @param sourceName the path or the decoded URI of the file
     */
    public static SheetDescriptor unreadSheet(String sourceName) {
        return new MarkdownSheet(sheetNameOf(sourceName), new Object[0][]);
    }

    /**
     * Returns the name of the sheet of a Markdown file: the name of the folder the file is in, or the name of the
     * file when its path names no folder.
     */
    private static String sheetNameOf(String path) {
        var normalized = path.replace('\\', '/');
        var file = normalized.lastIndexOf('/');
        var folder = file > 0 ? normalized.substring(normalized.lastIndexOf('/', file - 1) + 1, file) : "";
        return folder.isEmpty() ? FileUtils.getBaseName(normalized) : folder;
    }

    private static String read(InputStream stream) {
        try {
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new ExcelParseException(e);
        }
    }

    @Override
    public List<SheetDescriptor> getSheets() {
        return List.of(sheet);
    }

    @Override
    public @Nullable Object[][] getCells(SheetDescriptor sheet) {
        return this.sheet.getCells();
    }

    @Override
    public boolean isUse1904Windowing() {
        return false;
    }

    @Override
    public TableStyles getTableStyles(SheetDescriptor sheet, IGridRegion tableRegion) {
        return TableStyles.empty(tableRegion);
    }

    @Override
    public void close() {
        // The file is read whole when the reader is created
    }
}
