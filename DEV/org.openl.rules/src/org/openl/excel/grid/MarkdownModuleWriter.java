package org.openl.excel.grid;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.temporal.Temporal;
import java.util.ArrayList;
import java.util.Date;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jspecify.annotations.Nullable;

import org.openl.excel.parser.markdown.MarkdownGrid;
import org.openl.excel.parser.markdown.MarkdownWriteException;
import org.openl.rules.lang.xls.IXlsTableNames;
import org.openl.rules.lang.xls.TableSyntaxNodeUtils;
import org.openl.rules.lang.xls.XlsHelper;
import org.openl.rules.lang.xls.XlsNodeTypes;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.lang.xls.syntax.WorkbookSyntaxNode;
import org.openl.rules.lang.xls.types.meta.MetaInfoReader;
import org.openl.rules.table.CompositeGrid;
import org.openl.rules.table.GridRegion;
import org.openl.rules.table.ICell;
import org.openl.rules.table.IGridRegion;
import org.openl.util.StringUtils;

/**
 * Writes the tables of a workbook into a module folder of Markdown files, a table to a file, so that the folder is read
 * back as the same tables on sheets of the same names.
 *
 * <p>A table goes into a file named after it, in the folder its sheet names: the module folder itself for a sheet named
 * as the folder, or a folder of the name of the sheet in it. A name taken by another file gets a number,
 * {@code premium1.md}. A table set up for the whole module — the Environment, the Constants — goes into the file of
 * its fixed name at the root of the folder. The Environment tables of a workbook are joined in that one file, the rows
 * of each after the rows of the one before, and so are its Constants tables: the engine reads the rows of all of them
 * alike. A Properties table is a table like any other, in the folder of its sheet, so one of a category keeps its
 * category.
 *
 * <p>A folder or a file holds no character a name of OpenL Studio cannot hold: such a character is written as
 * {@code _}, and the name of a sheet loses a space at either end and a dot at its end.
 *
 * <p>A file keeps the values of a table as text, its merged cells and its properties. It keeps no look, no comment and
 * no formula: a cell keeps the value its formula computed. A number the compiler read as a date is written as the
 * date, and a file the Environment table includes is named from the folder the table is written in.
 *
 * <p>A table of no kind OpenL knows, such as a note, is no rule: it is written as text, a line for each row, in the
 * {@code notes.md} documentation of its sheet. A table whose header names a kind OpenL knows is a table, even when it
 * could not be read.
 *
 * <p>Any other table a file cannot hold is left out, and the result tells why: a TBasic or a ColumnMatch table, built
 * by the indent of its cells; a table written in parts; a table whose layout has no Markdown form.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class MarkdownModuleWriter {

    private static final String EXTENSION = ".md";

    /** The name of the file of the text of the tables of no kind OpenL knows on a sheet. */
    private static final String NOTES = "notes";

    /**
     * The kinds of tables set up for the whole module whose tables are joined in one file, the rows of each after the
     * rows of the one before: the engine reads the rows of every such table of a module alike, in that order.
     */
    private static final Set<XlsNodeTypes> JOINED = EnumSet.of(XlsNodeTypes.XLS_ENVIRONMENT,
            XlsNodeTypes.XLS_CONSTANTS);

    /** The kinds of tables built by the indent of their cells, which a Markdown file does not keep. */
    private static final Set<XlsNodeTypes> INDENTED = EnumSet.of(XlsNodeTypes.XLS_TBASIC,
            XlsNodeTypes.XLS_COLUMN_MATCH);

    /** What the name of a file of a table may not hold: what OpenL Studio refuses in a name, and a space. */
    private static final Pattern NOT_IN_A_NAME = Pattern.compile("[\\\\/:;<>?*%'\\[\\]|\"\\s]+");

    /** What the name of the folder of a sheet may not hold: what OpenL Studio refuses in a name. */
    private static final Pattern NOT_IN_A_FOLDER_NAME = Pattern.compile("[\\\\/:;<>?*%'\\[\\]|\"]");

    /** What a name of OpenL Studio may not end with: a space or a dot. */
    private static final Pattern NOT_AT_THE_END = Pattern.compile("[\\s.]+$");

    /**
     * Writes the tables of a workbook into a module folder, which is created when it does not exist.
     *
     * @param workbook the workbook, as a module was compiled from it
     * @param folder   the module folder
     * @return the files written, and the tables left out with the reason
     * @throws IOException when a file cannot be written
     */
    public static Written write(WorkbookSyntaxNode workbook, Path folder) throws IOException {
        Files.createDirectories(folder);
        var files = new ArrayList<Path>();
        var leftOut = new ArrayList<LeftOut>();
        // The text of the tables of no kind OpenL knows, by the sheet they stand on
        var notes = new LinkedHashMap<String, StringBuilder>();
        // The Environment and the Constants tables, each kind joined in the one file of the module
        var joined = new LinkedHashMap<Path, List<TableSyntaxNode>>();
        for (var table : workbook.getTableSyntaxNodes()) {
            // A table of a kind OpenL knows that could not be read is a table still, its error kept
            if (table.getNodeType() == XlsNodeTypes.XLS_OTHER && !XlsHelper.isKnownTableHeader(headerOf(table))) {
                notes.computeIfAbsent(sheetOf(table), sheet -> new StringBuilder()).append(textOf(table));
            } else if (JOINED.contains(table.getNodeType())) {
                joined.computeIfAbsent(fileFor(table, folder), file -> new ArrayList<>()).add(table);
            } else {
                var reason = reasonToLeaveOut(table);
                if (reason.isEmpty()) {
                    var file = fileFor(table, folder);
                    reason = write(List.of(table), file);
                    if (reason.isEmpty()) {
                        files.add(file);
                    }
                }
                reason.ifPresent(why -> leftOut.add(new LeftOut(table.getUri(), headerOf(table), why)));
            }
        }
        for (var tables : joined.entrySet()) {
            var reason = write(tables.getValue(), tables.getKey());
            if (reason.isEmpty()) {
                files.add(tables.getKey());
            } else {
                tables.getValue().forEach(table -> leftOut.add(new LeftOut(table.getUri(), headerOf(table),
                        reason.get())));
            }
        }
        for (var sheetNotes : notes.entrySet()) {
            var file = fileOf(folder, sheetNotes.getKey(), NOTES);
            Files.createDirectories(file.getParent());
            Files.writeString(file, "# Notes\n\n" + sheetNotes.getValue(), StandardCharsets.UTF_8);
            files.add(file);
        }
        return new Written(List.copyOf(files), List.copyOf(leftOut));
    }

    /**
     * The text of a table of no kind OpenL knows, as documentation: a line for each row, its cells apart by a bar.
     */
    private static String textOf(TableSyntaxNode table) {
        var cells = table.getGridTable();
        var text = new StringBuilder();
        for (var row = 0; row < cells.getHeight(); row++) {
            var line = new ArrayList<String>();
            for (var column = 0; column < cells.getWidth(); column++) {
                var cell = cells.getCell(column, row);
                var merged = cell.getRegion();
                var value = merged == null || startsAt(merged, cell) ? cell.getStringValue() : null;
                if (StringUtils.isNotBlank(value)) {
                    line.add(value.strip());
                }
            }
            if (!line.isEmpty()) {
                text.append(String.join(" | ", line)).append('\n');
            }
        }
        return text.append('\n').toString();
    }

    private static @Nullable String sheetOf(TableSyntaxNode table) {
        var sheet = table.getXlsSheetSourceCodeModule();
        return sheet == null ? null : sheet.getSheetName();
    }

    /**
     * Returns the file a new table of a module folder goes into: a file named after the table, in the folder its sheet
     * names. A name taken by another file gets a number, and so does a table at the root of the folder named as a table
     * set up for the whole module.
     *
     * @param folder    the module folder
     * @param sheetName the sheet of the table: the module folder for its name or none, a folder named after it in the
     *                  module folder, the first one found at any depth, or a new one at its root, see
     *                  {@link #folderNameOf}
     * @param tableName the name of the table
     * @throws IllegalArgumentException when the sheet names a path
     */
    public static Path fileOf(Path folder, @Nullable String sheetName, String tableName) {
        var directory = directoryOf(folder, sheetName);
        var file = directory.resolve(tableName + EXTENSION);
        var taken = directory.equals(folder) && MarkdownFolderModule.isModuleTableName(tableName);
        for (var number = 1; taken || Files.exists(file); number++) {
            file = directory.resolve(tableName + number + EXTENSION);
            taken = false;
        }
        return file;
    }

    /**
     * Returns whether a sheet names the module folder itself: by the name of the folder, or by none.
     */
    public static boolean namesTheFolder(String folderName, @Nullable String sheetName) {
        var name = folderNameOf(sheetName);
        return name.isEmpty() || name.equals(folderName);
    }

    /**
     * Returns the name of the folder of a sheet: the name of the sheet as OpenL Studio can hold it.
     *
     * <p>A character a name of OpenL Studio cannot hold, such as {@code %} or {@code ;}, which a sheet name may hold, is
     * written as {@code _}. A space at either end and a dot at the end are dropped.
     *
     * @return the name, empty for no sheet and for a sheet of no character a name keeps
     */
    public static String folderNameOf(@Nullable String sheetName) {
        var name = NOT_IN_A_FOLDER_NAME.matcher(StringUtils.trimToEmpty(sheetName)).replaceAll("_");
        return NOT_AT_THE_END.matcher(name).replaceFirst("");
    }

    private static Path directoryOf(Path folder, @Nullable String sheetName) {
        if (sheetName != null && (sheetName.contains("/") || sheetName.contains("\\"))) {
            throw new IllegalArgumentException("The sheet '%s' names a path".formatted(sheetName));
        }
        var name = folderNameOf(sheetName);
        if (namesTheFolder(String.valueOf(folder.getFileName()), name)) {
            return folder;
        }
        try (var paths = Files.find(folder, Integer.MAX_VALUE,
                (path, attributes) -> attributes.isDirectory() && path.endsWith(name))) {
            return paths.sorted().findFirst().orElse(folder.resolve(name));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** Why a table gets no file for its kind: a table built by the indent of its cells or written in parts. */
    private static Optional<String> reasonToLeaveOut(TableSyntaxNode table) {
        if (INDENTED.contains(table.getNodeType())) {
            return Optional.of("The table is built by the indent of its cells, which a Markdown file does not keep");
        }
        if (table.getNodeType() == XlsNodeTypes.XLS_TABLEPART
                || table.getGridTable().getGrid() instanceof CompositeGrid) {
            return Optional.of("The table is written in parts, which a Markdown file does not keep");
        }
        return Optional.empty();
    }

    /** The file a table goes into. */
    private static Path fileFor(TableSyntaxNode table, Path folder) {
        var moduleTable = MarkdownFolderModule.moduleTableOf(table);
        if (moduleTable.isPresent()) {
            return folder.resolve(moduleTable.get().file());
        }
        return fileOf(folder, sheetOf(table), nameOf(table));
    }

    /**
     * Writes tables into a file: the first one whole, and the rows under the header of each one after it, which follow
     * the rows of the one before.
     *
     * @return why the tables have no Markdown form, or nothing when the file is written
     */
    private static Optional<String> write(List<TableSyntaxNode> tables, Path file) throws IOException {
        var grid = MarkdownGrid.create(file.toUri().toString(), text -> {
            Files.createDirectories(file.getParent());
            Files.writeString(file, text, StandardCharsets.UTF_8);
        });
        var shift = 0;
        for (var table : tables) {
            var first = shift == 0 ? 0 : 1;
            write(grid, table, first, shift - first);
            shift += table.getGridTable().getHeight() - first;
        }
        try {
            grid.save();
            return Optional.empty();
        } catch (MarkdownWriteException e) {
            return Optional.of(e.getMessage());
        }
    }

    /**
     * The value of a cell as the compiler read it. A number the compiler read as a date, which a workbook keeps when
     * the cell is not formatted as a date, is that date; a number it read as text is the text it read.
     */
    private static @Nullable Object valueOf(ICell cell, @Nullable MetaInfoReader metaInfo) {
        var value = cell.getObjectValue();
        if (!(value instanceof Number) || metaInfo == null) {
            return value;
        }
        var read = metaInfo.getMetaInfo(cell.getAbsoluteRow(), cell.getAbsoluteColumn());
        var type = read == null || read.getDataType() == null ? null : read.getDataType().getInstanceClass();
        if (type == null) {
            return value;
        }
        if (Date.class.isAssignableFrom(type) || Temporal.class.isAssignableFrom(type)) {
            return cell.getNativeDate();
        }
        return CharSequence.class.isAssignableFrom(type) ? cell.getStringValue() : value;
    }

    private static boolean isInclude(ICell cell) {
        return IXlsTableNames.INCLUDE_TABLE.equals(StringUtils.trimToNull(cell.getStringValue()));
    }

    /**
     * The path of a file a workbook includes, as the Environment file of its module folder reaches it: an include is
     * read from the folder of the file holding it, and the folder of the workbook is one level up from that file.
     */
    private static @Nullable String includeFromTheFolder(@Nullable String include) {
        if (StringUtils.isBlank(include) || include.contains(":") || include.startsWith("/")) {
            return include;
        }
        return "../" + include;
    }

    /**
     * Writes the rows of a table from the given one, each moved down by the given number of rows.
     */
    private static void write(MarkdownGrid grid, TableSyntaxNode table, int first, int shift) {
        var cells = table.getGridTable();
        var region = cells.getRegion();
        var environment = table.getNodeType() == XlsNodeTypes.XLS_ENVIRONMENT;
        var metaInfo = table.getMetaInfoReader();
        if (metaInfo != null) {
            metaInfo.prepare(region);
        }
        for (var row = first; row < cells.getHeight(); row++) {
            for (var column = 0; column < cells.getWidth(); column++) {
                var cell = cells.getCell(column, row);
                var value = environment && column == 1 && isInclude(cells.getCell(0, row))
                        ? includeFromTheFolder(cell.getStringValue())
                        : valueOf(cell, metaInfo);
                write(grid, cell, value, region, shift);
            }
        }
    }

    /**
     * Writes a cell of a table, and its merged range when the cell starts one, moved down by the given number of rows.
     * A cell a range covers is not written.
     */
    private static void write(MarkdownGrid grid, ICell cell, @Nullable Object value, IGridRegion region, int shift) {
        var merged = cell.getRegion();
        var row = cell.getAbsoluteRow() - region.getTop() + shift;
        var column = cell.getAbsoluteColumn() - region.getLeft();
        if (merged != null && !startsAt(merged, cell)) {
            return;
        }
        grid.setCellValue(column, row, value);
        if (merged != null) {
            grid.addMergedRegion(new GridRegion(row,
                    column,
                    Math.min(merged.getBottom(), region.getBottom()) - region.getTop() + shift,
                    Math.min(merged.getRight(), region.getRight()) - region.getLeft()));
        }
    }

    private static boolean startsAt(IGridRegion merged, ICell cell) {
        return merged.getTop() == cell.getAbsoluteRow() && merged.getLeft() == cell.getAbsoluteColumn();
    }

    /**
     * The name of a table, as its header gives it: the name of the rule, of the type or of the data. A test with no
     * name of its own is named after the rule it tests and its kind: {@code rateTest}, {@code rateRun}. A part of a
     * table the compiler could not read is named as the table.
     * <p>
     * A table the compiler rejects, such as a second rule of the same signature, is named the same way, so that its
     * file gets a number after the file of the table it repeats.
     */
    private static String nameOf(TableSyntaxNode table) {
        var header = StringUtils.trimToEmpty(headerOf(table));
        var words = header.split("\\s+");
        var name = switch (table.getNodeType()) {
            // Test rate rateCases (age): rateCases, the words after the name unread; Test rate: rateTest
            case XLS_TEST_METHOD, XLS_RUN_METHOD -> words.length > 2
                    ? words[2]
                    : String.join("", List.of(words).reversed());
            case XLS_OTHER -> words.length > 1 ? words[1] : "";
            default -> TableSyntaxNodeUtils.str2name(header, table.getNodeType());
        };
        var fileName = NOT_IN_A_NAME.matcher(name).replaceAll("_");
        return fileName.isEmpty() ? "table" : fileName;
    }

    private static @Nullable String headerOf(TableSyntaxNode table) {
        return table.getGridTable().getCell(0, 0).getStringValue();
    }

    /**
     * The files written for the tables of a workbook, and the tables left out.
     *
     * @param files   the files written, in the order of the tables
     * @param leftOut the tables a file cannot hold
     */
    public record Written(List<Path> files, List<LeftOut> leftOut) {
    }

    /**
     * A table a Markdown file cannot hold.
     *
     * @param uri    where the table stands in its workbook
     * @param header the text of its header
     * @param reason why a file cannot hold it
     */
    public record LeftOut(String uri, @Nullable String header, String reason) {
    }
}
