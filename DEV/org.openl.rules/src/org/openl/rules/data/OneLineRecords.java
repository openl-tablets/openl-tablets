package org.openl.rules.data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jspecify.annotations.Nullable;

import org.openl.rules.lang.xls.XlsNodeTypes;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.table.ICell;
import org.openl.rules.table.IGridTable;
import org.openl.rules.table.ILogicalTable;
import org.openl.rules.testmethod.TestSuiteMethod;
import org.openl.syntax.impl.IdentifierNode;
import org.openl.types.IOpenField;
import org.openl.util.StringUtils;

/**
 * The body of a Data, Test or Run table written with no merged cell, a line for each record.
 *
 * <p>A record of such a table may take several lines: the lines of a collection field give its elements, and a cell
 * merged over lines gives its value to each of them. Written a line for each record, each element of a collection
 * takes columns of its own, named by its index: the column {@code drivers.name} of a record of two drivers gives the
 * columns {@code drivers[0].name} and {@code drivers[1].name}, and the columns of a nested collection follow the
 * columns of the element they belong to. The engine reads both forms into the same objects.
 *
 * <p>The values of an array field that a record gives in several lines or cells are joined in one cell, apart by
 * commas. A field of one value keeps the value the engine reads: the last value of a column it reads line by line,
 * such as a column of a Data table whose records take several lines, and the first value otherwise. A cell merged
 * over lines or columns gives its value once. Each column made from a column keeps its title and its foreign key. A
 * column naming the key of the elements of a collection, {@code drivers._PK_}, is left out: the index tells the
 * elements apart.
 *
 * <p>A table is written so only when it has a record of several lines or a merged cell under its header and its
 * properties, and only when it was read without errors.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class OneLineRecords {

    /**
     * Writes the body of a Data, Test or Run table a line for each record.
     *
     * @param node the table, as the compiler read it
     * @return the body, or nothing when the table needs no rewriting or cannot be rewritten
     */
    public static Optional<Body> of(TableSyntaxNode node) {
        var type = node.getNodeType();
        if (type != XlsNodeTypes.XLS_DATA && type != XlsNodeTypes.XLS_TEST_METHOD
                && type != XlsNodeTypes.XLS_RUN_METHOD) {
            return Optional.empty();
        }
        var body = DataTableBindHelper.getTableBody(node);
        if (!(tableOf(node) instanceof Table table) || body == null || table.getDataModel() == null
                || table.getData() == null || table.getDataArray() == null) {
            return Optional.empty();
        }
        var horizontal = DataTableBindHelper.getHorizontalTable(body, table.getDataModel().getType());
        var data = table.getData();
        if (!hasMergesOrLines(horizontal, table)) {
            return Optional.empty();
        }
        var descriptorRows = DataTableBindHelper.getDescriptorRows(horizontal);
        var columns = columnsOf(table, data.getWidth());
        if (columns == null) {
            return Optional.empty();
        }
        var records = recordsOf(table, data, columns);
        if (records == null) {
            return Optional.empty();
        }
        var flat = new ArrayList<FlatColumn>();
        var written = Arrays.stream(columns).filter(column -> !column.elementKey()).toList();
        addColumns(written, 0, new int[0], records.elements(), flat);
        var rows = new ArrayList<List<@Nullable Value>>();
        rows.add(flat.stream().map(column -> fieldOf(column, descriptorRows)).toList());
        if (descriptorRows.getHeight() > 1) {
            rows.add(flat.stream().map(column -> cellOf(descriptorRows, column.column().index(), 1)).toList());
        }
        rows.add(flat.stream().map(column -> cellOf(data, column.column().index(), 0)).toList());
        for (var values : records.values()) {
            rows.add(flat.stream().map(column -> valueOf(column, values)).toList());
        }
        var cells = horizontal == body ? rows : transposed(rows, flat.size());
        return Optional.of(new Body(rowsAbove(node), cells));
    }

    /** The data table of a Data, Test or Run table. */
    private static @Nullable ITable tableOf(TableSyntaxNode node) {
        return switch (node.getMember()) {
            case DataOpenField field -> field.getTable();
            case TestSuiteMethod test -> test.getBoundNode() == null ? null : test.getBoundNode().getTable();
            case null, default -> null;
        };
    }

    /** Whether a record of a table takes several lines, or a cell of its body is merged. */
    private static boolean hasMergesOrLines(ILogicalTable body, Table table) {
        var cells = body.getSource();
        for (var row = 0; row < cells.getHeight(); row++) {
            for (var column = 0; column < cells.getWidth(); column++) {
                if (cells.getCell(column, row).getRegion() != null) {
                    return true;
                }
            }
        }
        var data = table.getData();
        return IntStream.range(0, table.getSize())
                .anyMatch(index -> data.getRows(table.firstRowOf(index), lastRowOf(table, data, index))
                        .getSource()
                        .getHeight() > 1);
    }

    private static int lastRowOf(Table table, ILogicalTable data, int index) {
        return index + 1 < table.getSize() ? table.firstRowOf(index + 1) - 1 : data.getHeight() - 1;
    }

    /**
     * The columns of a table.
     *
     * @return the columns, or {@code null} when the path of a column cannot be told from its field
     */
    private static Column @Nullable [] columnsOf(Table table, int width) {
        var columns = new Column[width];
        for (var index = 0; index < width; index++) {
            var column = columnOf(table, index);
            if (column == null) {
                return null;
            }
            columns[index] = column;
        }
        return columns;
    }

    /**
     * The column of a table at an index.
     *
     * @return the column, or {@code null} when its path cannot be told from its field
     */
    private static @Nullable Column columnOf(Table table, int index) {
        var descriptor = table.getColumnDescriptor(index);
        if (descriptor == null) {
            return new Column(index, false, false, false, List.of(), null);
        }
        var fields = descriptor.getField() instanceof FieldChain chain ? chain.getFields() : new IOpenField[0];
        var elementKey = Arrays.stream(fields)
                .anyMatch(field -> field instanceof CollectionElementWithMultiRowField element && element.isPkField());
        var collections = collectionsOf(fields);
        var tokens = descriptor.getFieldChainTokens();
        if (!elementKey && !collections.isEmpty() && (tokens == null || tokens.length != fields.length)) {
            return null;
        }
        // A column that reads every line of a record keeps the value of its last line, as the engine sets it
        var everyLine = table.isMultiRowData()
                ? descriptor.getField() != null && !descriptor.isReference()
                : Table.isMultiRowColumn(descriptor);
        return new Column(index,
                elementKey,
                descriptor.isValuesAnArray(),
                everyLine,
                collections,
                tokens == null ? null : List.of(tokens));
    }

    /** The collections a field takes the elements of in several lines, the key of the elements left out. */
    private static List<Collection> collectionsOf(IOpenField[] fields) {
        var collections = new ArrayList<Collection>();
        for (var position = 0; position < fields.length; position++) {
            if (fields[position] instanceof CollectionElementWithMultiRowField element && !element.isPkField()) {
                collections.add(new Collection(element.getFieldPathFromRoot(), position));
            }
        }
        return List.copyOf(collections);
    }

    /**
     * The values of the records of a table, each cell placed in the element of each collection its line defined.
     *
     * @return the records, or {@code null} when a cell with a value is placed in no element
     */
    private static @Nullable Records recordsOf(Table table, ILogicalTable data, Column[] columns) {
        var values = new ArrayList<Map<CellKey, Map<Long, ICell>>>();
        var elements = new HashSet<ElementKey>();
        for (var index = 0; index < table.getSize(); index++) {
            var first = table.firstRowOf(index);
            var height = lastRowOf(table, data, index) - first + 1;
            var indexes = table.elementIndexesOf(index);
            var recordValues = new HashMap<CellKey, Map<Long, ICell>>();
            for (var column : columns) {
                if (column.elementKey()) {
                    continue;
                }
                var cells = data.getSubtable(column.index(), first, 1, height).getSource();
                if (!placeCells(column, cells, indexes, recordValues, elements)) {
                    return null;
                }
            }
            values.add(recordValues);
        }
        return new Records(values, elements);
    }

    /**
     * Places the cells of a column of a record in the elements their lines defined.
     *
     * @return whether each cell with a value is placed
     */
    private static boolean placeCells(Column column,
                                      IGridTable cells,
                                      @Nullable Map<String, int[]> indexes,
                                      Map<CellKey, Map<Long, ICell>> values,
                                      Set<ElementKey> elements) {
        for (var line = 0; line < cells.getHeight(); line++) {
            var path = pathOf(column, indexes, line);
            for (var cellColumn = 0; cellColumn < cells.getWidth(); cellColumn++) {
                var origin = cells.getCell(cellColumn, line).getTopLeftCellFromRegion();
                if (StringUtils.isBlank(origin.getStringValue())) {
                    continue;
                }
                if (path == null) {
                    return false;
                }
                values.computeIfAbsent(new CellKey(column.index(), listOf(path)), key -> new LinkedHashMap<>())
                        .putIfAbsent(originKey(origin), origin);
                for (var depth = 1; depth <= path.length; depth++) {
                    elements.add(new ElementKey(column.collections().get(depth - 1).path(),
                            listOf(Arrays.copyOf(path, depth))));
                }
            }
        }
        return true;
    }

    /**
     * The index of the element of each collection of a column that a line of a record defined.
     *
     * @return the indexes, from the outer collection, or {@code null} when the line defined no element of one of them
     */
    private static int @Nullable [] pathOf(Column column, @Nullable Map<String, int[]> indexes, int line) {
        var path = new int[column.collections().size()];
        for (var depth = 0; depth < path.length; depth++) {
            var lines = indexes == null ? null : indexes.get(column.collections().get(depth).path());
            if (lines == null || line >= lines.length || lines[line] < 0) {
                return null;
            }
            path[depth] = lines[line];
        }
        return path;
    }

    private static long originKey(ICell cell) {
        return ((long) cell.getAbsoluteRow() << 32) | cell.getAbsoluteColumn();
    }

    /**
     * Adds the columns of the elements of the collections the given columns name, from a depth of their collections:
     * the columns of each element of a collection follow one another, from the element of index 0 to the last element
     * a record defined, in the place of the first column of the collection.
     */
    private static void addColumns(List<Column> columns,
                                   int depth,
                                   int[] path,
                                   Set<ElementKey> elements,
                                   List<FlatColumn> flat) {
        var added = new HashSet<String>();
        for (var column : columns) {
            if (column.collections().size() == depth) {
                flat.add(new FlatColumn(column, listOf(path)));
            } else if (added.add(column.collections().get(depth).path())) {
                var collection = column.collections().get(depth).path();
                var members = columns.stream()
                        .filter(other -> other.collections().size() > depth
                                && other.collections().get(depth).path().equals(collection))
                        .toList();
                var last = lastIndexOf(collection, path, elements);
                for (var index = 0; index <= last; index++) {
                    var elementPath = Arrays.copyOf(path, depth + 1);
                    elementPath[depth] = index;
                    addColumns(members, depth + 1, elementPath, elements, flat);
                }
            }
        }
    }

    /** The index of the last element of a collection the records define; {@code 0} when they define none. */
    private static int lastIndexOf(String collection, int[] outer, Set<ElementKey> elements) {
        var last = 0;
        for (var element : elements) {
            var path = element.path();
            if (element.collection().equals(collection) && path.size() == outer.length + 1
                    && IntStream.range(0, outer.length).allMatch(depth -> path.get(depth) == outer[depth])) {
                last = Math.max(last, path.getLast());
            }
        }
        return last;
    }

    /** The field of a column: the field of the table, the index of each element added to the name of its collection. */
    private static @Nullable Value fieldOf(FlatColumn flat, ILogicalTable descriptorRows) {
        var column = flat.column();
        if (column.collections().isEmpty()) {
            return cellOf(descriptorRows, column.index(), 0);
        }
        var names = column.tokens().stream().map(IdentifierNode::getIdentifier).toArray(String[]::new);
        for (var depth = 0; depth < column.collections().size(); depth++) {
            var position = column.collections().get(depth).position();
            names[position] = names[position].strip() + "[" + flat.path().get(depth) + "]";
        }
        return new Text(Arrays.stream(names).map(String::strip).collect(Collectors.joining(".")));
    }

    private static @Nullable Value cellOf(ILogicalTable rows, int column, int row) {
        var cell = rows.getSubtable(column, row, 1, 1).getSource().getCell(0, 0);
        return StringUtils.isBlank(cell.getStringValue()) ? null : new Cell(cell);
    }

    /** The value of a column of a record: the first value it gives, or each one for an array field. */
    private static @Nullable Value valueOf(FlatColumn flat, Map<CellKey, Map<Long, ICell>> recordCells) {
        var cells = recordCells.get(new CellKey(flat.column().index(), flat.path()));
        if (cells == null || cells.isEmpty()) {
            return null;
        }
        var origins = List.copyOf(cells.values());
        if (origins.size() == 1) {
            return new Cell(origins.getFirst());
        }
        if (flat.column().array()) {
            return new Joined(origins);
        }
        return new Cell(flat.column().everyLine() ? origins.getLast() : origins.getFirst());
    }

    private static List<Integer> listOf(int[] path) {
        return Arrays.stream(path).boxed().toList();
    }

    private static List<List<@Nullable Value>> transposed(List<List<@Nullable Value>> rows, int width) {
        var columns = new ArrayList<List<@Nullable Value>>(width);
        for (var column = 0; column < width; column++) {
            var cells = new ArrayList<@Nullable Value>(rows.size());
            for (var row : rows) {
                cells.add(row.get(column));
            }
            columns.add(cells);
        }
        return columns;
    }

    /** The number of the rows of the sheet above the body of a table: its header and its properties. */
    private static int rowsAbove(TableSyntaxNode node) {
        var table = node.getTable();
        var startRow = node.hasPropertiesDefinedInTable() ? 2 : 1;
        var rows = 0;
        for (var row = 0; row < startRow; row++) {
            rows += table.getRowHeight(row);
        }
        return rows;
    }

    /**
     * The body of a table, a line for each record.
     *
     * @param row   the first row of the body in the table, under its header and its properties
     * @param cells the cells of the body by row, in the orientation of the table; {@code null} for a blank cell
     */
    public record Body(int row, List<List<@Nullable Value>> cells) {
    }

    /** The value of a cell of the body. */
    public sealed interface Value {
    }

    /**
     * A cell of the table, the value of the cell written.
     */
    public record Cell(ICell cell) implements Value {
    }

    /**
     * The cells of the table that give the values of an array field, written in one cell apart by commas.
     */
    public record Joined(List<ICell> cells) implements Value {

        /** The separator of the values of an array in a cell. */
        public static final String SEPARATOR = ", ";
    }

    /** A text of its own, such as the field of a column of an element. */
    public record Text(String text) implements Value {
    }

    /**
     * A collection a column takes the elements of in several lines.
     *
     * @param path     the path of the collection from the root object
     * @param position the place of the collection in the field of the column
     */
    private record Collection(String path, int position) {
    }

    /**
     * A column of a table.
     *
     * @param elementKey whether the column names the key of the elements of a collection
     * @param array      whether the field of the column holds an array
     * @param everyLine  whether the engine reads every line of a record in the column, the last value staying
     */
    private record Column(int index,
                          boolean elementKey,
                          boolean array,
                          boolean everyLine,
                          List<Collection> collections,
                          @Nullable List<IdentifierNode> tokens) {
    }

    /** A column written for the element of each collection of a column given by its index. */
    private record FlatColumn(Column column, List<Integer> path) {
    }

    private record CellKey(int column, List<Integer> path) {
    }

    private record ElementKey(String collection, List<Integer> path) {
    }

    /**
     * The records of a table.
     *
     * @param values   the cells of each record by the column and the element they are written in, each cell once
     * @param elements the elements the records define
     */
    private record Records(List<Map<CellKey, Map<Long, ICell>>> values, Set<ElementKey> elements) {
    }
}
