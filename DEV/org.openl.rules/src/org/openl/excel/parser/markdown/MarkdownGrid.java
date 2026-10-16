package org.openl.excel.parser.markdown;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import lombok.Getter;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellRangeAddress;
import org.jspecify.annotations.Nullable;

import org.openl.rules.lang.xls.XlsSheetSourceCodeModule;
import org.openl.rules.lang.xls.XlsWorkbookSourceCodeModule;
import org.openl.rules.lang.xls.load.CellLoader;
import org.openl.rules.lang.xls.load.SheetLoader;
import org.openl.rules.table.AGrid;
import org.openl.rules.table.GridRegion;
import org.openl.rules.table.ICell;
import org.openl.rules.table.IGridRegion;
import org.openl.rules.table.IWritableGrid;
import org.openl.rules.table.ui.ICellStyle;
import org.openl.rules.table.xls.PoiExcelHelper.ThemedColor;
import org.openl.rules.table.xls.writers.XlsCellArrayWriter;
import org.openl.util.FileUtils;
import org.openl.util.StringTool;
import org.openl.util.StringUtils;

/**
 * The sheet of a Markdown file, opened for the table editor to write and saved back into the file.
 *
 * <p>The sheet holds the table the way the file is read: the header, the properties and the body, with the merged
 * ranges. A save writes the table into the text of the file as it was when the sheet was opened. Only the front
 * matter and the body change, and only where the table did. A file another program changed since the module read it
 * is not saved over, as a workbook is not.
 *
 * <p>Values are kept as text, the way the file holds them: a number, a date or a list written into a cell becomes the
 * text the table reads back as the same value. The file keeps no formatting, so styles, fonts, borders and comments
 * are not kept, and it holds no Excel formulas, which are refused. Nothing reaches the file before the save.
 */
public final class MarkdownGrid extends AGrid implements IWritableGrid {

    /** The address of the sheet, which the tables written on it are known by. */
    @Getter
    private final String uri;
    /** The sheet of the file the module compiles, or {@code null} for a file written outside a module. */
    @Getter
    private final @Nullable XlsSheetSourceCodeModule sheetSource;
    /** The name of the file the error messages tell. */
    private final String fileName;
    /** The text of the file when the sheet was opened, or {@code null} for a file that does not exist yet. */
    private final @Nullable String text;
    private final Saver saver;
    private final List<List<@Nullable String>> rows = new ArrayList<>();
    private final List<IGridRegion> merges = new ArrayList<>();

    private MarkdownGrid(String uri,
                         @Nullable XlsSheetSourceCodeModule sheetSource,
                         String fileName,
                         @Nullable String text,
                         SheetCells cells,
                         Saver saver) {
        this.uri = uri;
        this.sheetSource = sheetSource;
        this.fileName = fileName;
        this.text = text;
        this.saver = saver;
        for (var row = 0; row < cells.values().length; row++) {
            for (var column = 0; column < cells.values()[row].length; column++) {
                put(column, row, cells.values()[row][column]);
            }
        }
        cells.merges().forEach(range -> merges.add(new GridRegion(range.getFirstRow(),
                range.getFirstColumn(),
                range.getLastRow(),
                range.getLastColumn())));
    }

    /**
     * Opens the sheet of a Markdown file for writing, as the file stands now.
     *
     * @param sheetSource the sheet of the file
     * @throws MarkdownFormatException when the file breaks the format
     * @throws MarkdownWriteException  when the file holds no table
     */
    public static MarkdownGrid open(XlsSheetSourceCodeModule sheetSource) {
        var workbook = sheetSource.getWorkbookSource();
        String text;
        try (var stream = workbook.getByteStream()) {
            text = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        var withoutMark = !text.isEmpty() && text.charAt(0) == MarkdownReader.BYTE_ORDER_MARK
                ? text.substring(1)
                : text;
        var file = MarkdownTableParser.parse(withoutMark, workbook.getDisplayName())
                .orElseThrow(() -> new MarkdownWriteException(
                        "%s holds no table to write".formatted(workbook.getDisplayName())));
        return new MarkdownGrid(sheetSource.getUri(),
                sheetSource,
                workbook.getDisplayName(),
                text,
                SheetLayout.cellsOf(file.table()),
                written -> workbook.save(out -> out.write(written.getBytes(StandardCharsets.UTF_8))));
    }

    /**
     * Opens the sheet of a Markdown file that does not exist yet, for a new table to be written on.
     *
     * <p>The save hands the text of the file to the saver, which creates the file. The sheet is compiled by no
     * module, so a table written on it is known by no module either.
     *
     * @param fileUri the address the file is compiled under, see {@link #uriOf}
     * @param saver   creates the file with the text it is given
     */
    public static MarkdownGrid create(String fileUri, Saver saver) {
        var path = StringTool.decodeURL(fileUri);
        var uri = fileUri + "?sheet=" + StringTool.encodeURL(MarkdownReader.sheetNameOf(path));
        return new MarkdownGrid(uri, null, FileUtils.getName(path), null, new SheetCells(new String[0][], List.of()),
                saver);
    }

    /**
     * Opens the sheet of a new file of a module folder, for a new table to be written on.
     *
     * <p>The sheet is the one the module compiles the file as once it is written, so a table written on it is known
     * as the module will know it. The address of the file is read off a file the folder holds.
     *
     * @param sibling a file of the module folder, as it was compiled
     * @param folder  the module folder
     * @param file    the new file
     * @param saver   creates the file with the text it is given
     */
    public static MarkdownGrid createIn(XlsWorkbookSourceCodeModule sibling, Path folder, Path file, Saver saver) {
        var fileUri = uriOf(sibling, folder, file);
        var path = StringTool.decodeURL(fileUri);
        var workbook = new XlsWorkbookSourceCodeModule(new MarkdownFileSource(file, fileUri));
        var sheet = new XlsSheetSourceCodeModule(new UnwrittenSheet(MarkdownReader.sheetNameOf(path)), workbook);
        return new MarkdownGrid(sheet.getUri(), sheet, FileUtils.getName(path), null,
                new SheetCells(new String[0][], List.of()), saver);
    }

    /** The sheet of a file not written yet: it has a name, and nothing to read. */
    private record UnwrittenSheet(String name) implements SheetLoader {

        @Override
        public Sheet getSheet() {
            throw new IllegalStateException("The file of the sheet '%s' is not written yet".formatted(name));
        }

        @Override
        public String getSheetName() {
            return name;
        }

        @Override
        public CellLoader getCellLoader(int column, int row) {
            throw new IllegalStateException("The file of the sheet '%s' is not written yet".formatted(name));
        }
    }

    /**
     * Returns the address a new file of a module folder is compiled under, read off a file the folder holds.
     *
     * @param sibling a file of the module folder, as it was compiled
     * @param folder  the module folder
     * @param file    the new file
     */
    public static String uriOf(XlsWorkbookSourceCodeModule sibling, Path folder, Path file) {
        var siblingPath = Path.of(URI.create(sibling.getFileUri())).toAbsolutePath().normalize();
        var siblingTail = uriPath(folder.relativize(siblingPath).toString().replace('\\', '/'));
        var siblingUri = sibling.getUri();
        if (!siblingUri.endsWith(siblingTail)) {
            throw new IllegalArgumentException("%s is not a file of %s".formatted(siblingUri, folder));
        }
        var base = siblingUri.substring(0, siblingUri.length() - siblingTail.length());
        return base + uriPath(folder.relativize(file.toAbsolutePath().normalize()).toString().replace('\\', '/'));
    }

    /**
     * Writes a relative path as a URI path, escaping what a URI cannot hold, such as spaces.
     */
    public static String uriPath(String path) {
        try {
            return new URI(null, null, path, null).toASCIIString();
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(e);
        }
    }

    /**
     * Creates or rewrites the file of a sheet with its text.
     */
    @FunctionalInterface
    public interface Saver {

        /**
         * Writes the file.
         *
         * @param text the text of the file
         * @throws IOException when the file cannot be written
         */
        void save(String text) throws IOException;
    }

    /**
     * Writes the table into its file.
     *
     * @throws MarkdownWriteException when the table has no Markdown form
     */
    @Override
    public void save() throws IOException {
        var values = rows.stream().map(row -> row.toArray(String[]::new)).toArray(String[][]::new);
        var ranges = merges.stream()
                .map(range -> new CellRangeAddress(range.getTop(),
                        range.getBottom(),
                        range.getLeft(),
                        range.getRight()))
                .toList();
        var cells = new SheetCells(values, ranges);
        saver.save(text == null
                ? MarkdownTableWriter.newFile(SheetLayout.tableOf(cells))
                : MarkdownTableWriter.write(text, fileName, cells));
    }

    // Reading

    @Override
    public ICell getCell(int column, int row) {
        return new MarkdownCell(column, row, this);
    }

    /**
     * Returns the value a cell holds itself, or {@code null} for a blank cell and a cell a merged range covers.
     */
    @Nullable
    String valueAt(int column, int row) {
        return row < rows.size() && column < rows.get(row).size() ? rows.get(row).get(column) : null;
    }

    @Override
    public int getColumnWidth(int col) {
        return 0;
    }

    @Override
    public int getMaxColumnIndex(int row) {
        return row < rows.size() ? rows.get(row).size() - 1 : 0;
    }

    @Override
    public int getMaxRowIndex() {
        return rows.size() - 1;
    }

    @Override
    public int getMinColumnIndex(int row) {
        return 0;
    }

    @Override
    public int getMinRowIndex() {
        return 0;
    }

    @Override
    public IGridRegion getMergedRegion(int i) {
        return merges.get(i);
    }

    @Override
    public int getNumberOfMergedRegions() {
        return merges.size();
    }

    @Override
    public boolean isEmpty(int col, int row) {
        return StringUtils.isBlank(valueAt(col, row));
    }

    // Writing

    @Override
    public void setCellValue(int col, int row, @Nullable Object value) {
        put(col, row, textOf(value));
    }

    @Override
    public void setCellStringValue(int col, int row, @Nullable String value) {
        put(col, row, value);
    }

    /**
     * Refuses an Excel formula: a Markdown file holds no Excel formulas.
     *
     * <p>An OpenL formula is a text starting with {@code =}, which {@link #setCellValue} writes as it is.
     *
     * @throws MarkdownWriteException always, and the cell is left as it is
     */
    @Override
    public void setCellFormula(int col, int row, @Nullable String formula) {
        var excelFormula = formula == null || formula.startsWith("=") ? formula : "=" + formula;
        throw new MarkdownWriteException(
                "The Excel formula '%s' cannot be written in a Markdown table, which holds values and OpenL formulas"
                        .formatted(excelFormula));
    }

    @Override
    public void createCell(int col,
                           int row,
                           @Nullable Object value,
                           @Nullable String formula,
                           @Nullable ICellStyle style,
                           @Nullable String comment,
                           @Nullable String prevCommentAuthor) {
        if (StringUtils.isNotBlank(formula)) {
            setCellFormula(col, row, formula);
        } else {
            setCellValue(col, row, value);
        }
    }

    @Override
    public void clearCell(int col, int row) {
        put(col, row, null);
    }

    @Override
    public void copyCell(int colFrom, int rowFrom, int colTo, int rowTo) {
        put(colTo, rowTo, valueAt(colFrom, rowFrom));
    }

    /** Copies the text of a cell as the file holds it: the file keeps nothing else of a cell. */
    @Override
    public void copyCellValue(int colFrom, int rowFrom, int colTo, int rowTo) {
        copyCell(colFrom, rowFrom, colTo, rowTo);
    }

    /**
     * Moves a merged range. The cells it covers below and right of its top left cell are cleared, and the top left
     * cell keeps its text.
     */
    @Override
    public void moveMergedRegion(IGridRegion from, IGridRegion to) {
        removeMergedRegion(from);
        for (var row = to.getTop(); row <= to.getBottom(); row++) {
            for (var column = to.getLeft(); column <= to.getRight(); column++) {
                if (row != to.getTop() || column != to.getLeft()) {
                    put(column, row, null);
                }
            }
        }
        merges.add(new GridRegion(to));
    }

    /**
     * Merges the cells of a range. As in Excel, the first value of the range, read row by row, moves to its top left
     * cell, and the other cells are cleared.
     */
    @Override
    public int addMergedRegion(IGridRegion reg) {
        var value = firstValue(reg);
        for (var row = reg.getTop(); row <= reg.getBottom(); row++) {
            for (var column = reg.getLeft(); column <= reg.getRight(); column++) {
                put(column, row, null);
            }
        }
        put(reg.getLeft(), reg.getTop(), value);
        merges.add(new GridRegion(reg));
        return merges.size() - 1;
    }

    private @Nullable String firstValue(IGridRegion reg) {
        for (var row = reg.getTop(); row <= reg.getBottom(); row++) {
            for (var column = reg.getLeft(); column <= reg.getRight(); column++) {
                var value = valueAt(column, row);
                if (value != null) {
                    return value;
                }
            }
        }
        return null;
    }

    @Override
    public void removeMergedRegion(IGridRegion to) {
        removeMergedRegion(to.getLeft(), to.getTop());
    }

    @Override
    public void removeMergedRegion(int x, int y) {
        merges.removeIf(range -> range.getLeft() == x && range.getTop() == y);
    }

    /**
     * Returns the top left corner of an empty sheet: the file holds one table, so a sheet holding one has no room.
     */
    @Override
    public @Nullable IGridRegion findEmptyRect(int width, int height) {
        return rows.isEmpty() ? new GridRegion(0, 0, height - 1, width - 1) : null;
    }

    private void put(int column, int row, @Nullable String value) {
        if (value == null && valueAt(column, row) == null) {
            return;
        }
        while (rows.size() <= row) {
            rows.add(new ArrayList<>());
        }
        var cells = rows.get(row);
        if (cells.size() <= column) {
            cells.addAll(Collections.nCopies(column + 1 - cells.size(), null));
        }
        cells.set(column, value);
    }

    /**
     * Returns the text a value is written as, which the table reads back as the same value.
     */
    static @Nullable String textOf(@Nullable Object value) {
        return switch (value) {
            case null -> null;
            case String text -> text;
            case Date date -> textOf(date);
            case Double number when Double.isFinite(number) -> plain(BigDecimal.valueOf(number));
            case Float number when Float.isFinite(number) -> plain(new BigDecimal(number.toString()));
            case BigDecimal number -> plain(number);
            case Enum<?> constant -> constant.name();
            case Object[] values -> XlsCellArrayWriter.serialize(values);
            default -> value.toString();
        };
    }

    /**
     * Returns a date as the day alone at midnight, and with its time otherwise, in the time zone the table is read in:
     * a date written with no zone is read back in the zone of the reader, so every date type reads it as written.
     */
    private static String textOf(Date date) {
        var time = LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());
        return time.toLocalTime().equals(LocalTime.MIDNIGHT)
                ? time.toLocalDate().toString()
                : DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(time);
    }

    private static String plain(BigDecimal number) {
        return number.stripTrailingZeros().toPlainString();
    }

    // The file keeps no formatting

    @Override
    public void setCellStyle(int col, int row, @Nullable ICellStyle style) {
        // Not kept
    }

    @Override
    public void setCellBorderStyle(int col, int row, @Nullable ICellStyle style) {
        // Not kept
    }

    @Override
    public void setCellAlignment(int col, int row, @Nullable HorizontalAlignment alignment) {
        // Not kept
    }

    @Override
    public void setCellIndent(int col, int row, int indent) {
        // Not kept
    }

    @Override
    public void setCellFillColor(int col, int row, short @Nullable [] color) {
        // Not kept
    }

    @Override
    public void setCellFillColor(int col, int row, @Nullable ThemedColor color) {
        // Not kept
    }

    @Override
    public void setCellFontBold(int col, int row, boolean bold) {
        // Not kept
    }

    @Override
    public void setCellFontItalic(int col, int row, boolean italic) {
        // Not kept
    }

    @Override
    public void setCellFontUnderline(int col, int row, boolean underlined) {
        // Not kept
    }

    @Override
    public void setCellFontColor(int col, int row, short @Nullable [] color) {
        // Not kept
    }

    @Override
    public void setCellFontColor(int col, int row, @Nullable ThemedColor color) {
        // Not kept
    }

    @Override
    public void setCellComment(int col, int row, @Nullable String comment, @Nullable String prevCommentAuthor) {
        // Not kept
    }
}
