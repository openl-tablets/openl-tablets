package org.openl.rules.lang.xls.types.meta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import lombok.extern.slf4j.Slf4j;

import org.openl.binding.impl.NodeType;
import org.openl.binding.impl.SimpleNodeUsage;
import org.openl.exception.OpenLCompilationException;
import org.openl.rules.binding.RuleRowHelper;
import org.openl.rules.data.ColumnDescriptor;
import org.openl.rules.data.DataNodeBinder;
import org.openl.rules.data.DataTableBoundNode;
import org.openl.rules.data.ForeignKeyColumnDescriptor;
import org.openl.rules.data.ITable;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.lang.xls.types.CellMetaInfo;
import org.openl.rules.table.IGridRegion;
import org.openl.rules.table.openl.GridCellSourceCodeModule;
import org.openl.syntax.exception.SyntaxNodeException;
import org.openl.syntax.impl.IdentifierNode;
import org.openl.syntax.impl.Tokenizer;
import org.openl.types.IOpenClass;
import org.openl.types.java.JavaOpenClass;

@Slf4j
public class DataTableMetaInfoReader extends BaseMetaInfoReader<DataTableBoundNode> {

    /**
     * What each column of the table holds, worked out once.
     *
     * <p>Every cell of a column holds the same thing, and the table is bound before this reader is asked
     * anything, so the answer cannot change while the reader lives. Working it out per cell instead would ask
     * a column that reads a foreign key for the domain of that whole foreign table once for every cell of
     * this one.
     */
    private final Map<ColumnDescriptor, CellMetaInfo> columns = new ConcurrentHashMap<>();

    public DataTableMetaInfoReader(DataTableBoundNode boundNode) {
        super(boundNode);
    }

    @Override
    protected TableSyntaxNode getTableSyntaxNode() {
        return getBoundNode().getTableSyntaxNode();
    }

    /**
     * The columns the table declares, each holding what its header says however many rows follow.
     * <p>
     * A column is answered for whether or not anybody has written in it, so the answer holds for a table whose
     * columns are declared and whose body is empty, and for a row laid down in one.
     */
    @Override
    public List<TableArea> getAreas() {
        var table = getBoundNode().getTable();
        if (table == null) {
            // Datatype contains errors
            return List.of();
        }
        var data = table.getData();
        var normalOrientation = data.isNormalOrientation();
        var region = getTableSyntaxNode().getGridTable().getRegion();
        var dataFrom = dataFrom(table, region);
        var areas = new ArrayList<TableArea>();
        for (ColumnDescriptor descriptor : table.getDataModel().getDescriptors()) {
            var column = descriptor.getColumnIdx();
            if (column >= data.getWidth()) {
                continue;
            }
            // A column of several values is written over as many of the workbook's own lines as it holds.
            var width = data.getColumnWidth(column);
            CellMetaInfo metaInfo;
            try {
                metaInfo = getColumnMetaInfo(table, descriptor, width);
            } catch (SyntaxNodeException e) {
                log.error(e.getMessage(), e);
                continue;
            }
            if (metaInfo == null) {
                continue;
            }
            var cell = data.getCell(column, 0);
            var at = normalOrientation ? cell.getAbsoluteColumn() - region.getLeft()
                    : cell.getAbsoluteRow() - region.getTop();
            // Down a column and on past the last row, or across a row of a table written the other way round.
            areas.add(normalOrientation
                    ? new TableArea(dataFrom, at, TableArea.TO_THE_END, width, metaInfo)
                    : new TableArea(at, dataFrom, width, TableArea.TO_THE_END, metaInfo));
        }
        return areas;
    }

    /** How many cells of a column its headings take: the table's own rows above the data, and the titles. */
    private static int dataFrom(ITable table, IGridRegion region) {
        var data = table.getData();
        if (data.getHeight() == 0) {
            return 0;
        }
        var first = data.getCell(0, 0);
        // The data begins under the titles, where the table is written with them.
        var titles = table.getDataModel().hasColumnTitleRow() ? data.getRowHeight(0) : 0;
        return titles + (data.isNormalOrientation() ? first.getAbsoluteRow() - region.getTop()
                : first.getAbsoluteColumn() - region.getLeft());
    }

    @Override
    protected CellMetaInfo getHeaderMetaInfo() {
        var table = getTableSyntaxNode().getTable();
        var source = new GridCellSourceCodeModule(table.getSource(), null);

        var boundNode = getBoundNode();
        if (boundNode.getField() == null) {
            // Datatype contains errors
            return null;
        }

        var typeMeta = boundNode.getType().getMetaInfo();
        if (typeMeta != null) {
            try {
                IdentifierNode[] parsedHeader = Tokenizer.tokenize(source, " \n\r");
                return RuleRowHelper
                        .createCellMetaInfo(parsedHeader[DataNodeBinder.TYPE_INDEX], typeMeta, NodeType.DATATYPE);
            } catch (OpenLCompilationException e) {
                log.error(e.getMessage(), e);
                return null;
            }
        }

        return null;
    }

    @Override
    protected CellMetaInfo getBodyMetaInfo(int row, int col) {
        try {
            var table = getBoundNode().getTable();
            if (table == null) {
                // Datatype contains errors
                return null;
            }

            if (isDescription(table, row, col)) {
                return getDescriptionMetaInfo(table, row, col);
            }

            if (table.getNumberOfRows() > 0 && table.getNumberOfColumns() > 0) {
                // Data exist
                return getDataMetaInfo(table, row, col);
            }

            return null;
        } catch (SyntaxNodeException e) {
            log.error(e.getMessage(), e);
            return null;
        }
    }

    private static boolean isDescription(ITable table, int row, int col) {
        if (table.getNumberOfRows() == 0) {
            // No data values in this table. Only description.
            return true;
        }
        var firstDataCell = table.getRowTable(0).getCell(0, 0);
        if (table.getData().isNormalOrientation()) {
            return row < firstDataCell.getAbsoluteRow();
        } else {
            return col < firstDataCell.getAbsoluteColumn();
        }
    }

    private CellMetaInfo getDescriptionMetaInfo(ITable table, int row, int col) {
        var numberOfColumns = table.getNumberOfColumns();
        for (var i = 0; i < numberOfColumns; i++) {
            var descriptor = table.getColumnDescriptor(i);
            var metaInfo = checkForeignKeyInHeader(descriptor, row, col);
            if (metaInfo != NOT_FOUND) {
                return metaInfo;
            }
        }

        return null;
    }

    private CellMetaInfo checkForeignKeyInHeader(ColumnDescriptor descriptor, int row, int col) {
        if (descriptor instanceof ForeignKeyColumnDescriptor foreignDescriptor) {
            var cellKey = foreignDescriptor.getForeignKeyCellCoordinate();
            if (isNeededCell(cellKey, row, col)) {
                // Found needed cell
                if (foreignDescriptor.isReference()) {
                    var db = getBoundNode().getDataBase();
                    var foreignKeyTable = foreignDescriptor.getForeignKeyTable();
                    var foreignTable = db.getTable(foreignKeyTable.getIdentifier());
                    if (foreignTable != null) {
                        var nodeUsage = new SimpleNodeUsage(foreignKeyTable,
                                foreignTable.getTableSyntaxNode().getHeaderLineValue().getValue(),
                                foreignTable.getTableSyntaxNode().getUri(),
                                NodeType.DATA);
                        return new CellMetaInfo(JavaOpenClass.STRING, false, List.of(nodeUsage));
                    }

                }
                return null;
            }
        }

        return NOT_FOUND;
    }

    private CellMetaInfo getDataMetaInfo(ITable table, int row, int col) throws SyntaxNodeException {
        var data = table.getData();
        var normalOrientation = data.isNormalOrientation();

        var firstCell = table.getRowTable(0).getCell(0, 0);
        // logicalCol is column for normal orientation and is row for transposed table
        int logicalCol = normalOrientation ? (col - firstCell.getAbsoluteColumn()) : (row - firstCell.getAbsoluteRow());

        for (ColumnDescriptor descriptor : table.getDataModel().getDescriptors()) {
            var column = descriptor.getColumnIdx();
            if (column >= data.getWidth()) {
                continue;
            }
            var logicalColStart = data.getCell(column, 0).getColumn();
            var logicalWidth = data.getColumnWidth(column);

            if (logicalColStart <= logicalCol && logicalCol < logicalColStart + logicalWidth) {
                // Found needed column for cell
                return getColumnMetaInfo(table, descriptor, logicalWidth);
            }
        }
        return null;
    }

    /** What every cell of the column holds, worked out once and kept; see {@link #columns}. */
    private CellMetaInfo getColumnMetaInfo(ITable table, ColumnDescriptor descriptor,
            int logicalWidth) throws SyntaxNodeException {
        var kept = columns.get(descriptor);
        if (kept == null) {
            kept = columnMetaInfo(table, descriptor, logicalWidth);
            columns.put(descriptor, kept == null ? NOT_FOUND : kept);
        }
        return kept == NOT_FOUND ? null : kept;
    }

    /** What the type the column was declared with says its cells hold, and whether a cell holds many of them. */
    private CellMetaInfo columnMetaInfo(ITable table, ColumnDescriptor descriptor,
            int logicalWidth) throws SyntaxNodeException {
        IOpenClass columnType;
        if (descriptor instanceof ForeignKeyColumnDescriptor columnDescriptor) {
            var db = getBoundNode().getDataBase();
            columnType = columnDescriptor.getDomainClassForForeignTable(db);
        } else {
            // A column standing for the row itself — `this` — holds what the table is a table of.
            columnType = descriptor.isConstructor() ? table.getDataModel().getType() : descriptor.getType();
        }
        if (columnType == null) {
            return null;
        }
        if (!descriptor.isValuesAnArray()) {
            return new CellMetaInfo(columnType, false);
        }
        if (descriptor instanceof ForeignKeyColumnDescriptor) {
            return new CellMetaInfo(columnType, logicalWidth == 1);
        }
        var elemType = columnType.getAggregateInfo().getComponentType(columnType);
        return new CellMetaInfo(elemType, logicalWidth == 1);
    }
}
