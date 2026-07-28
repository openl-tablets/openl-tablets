package org.openl.rules.lang.xls.types.meta;

import java.util.List;

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
import org.openl.rules.table.openl.GridCellSourceCodeModule;
import org.openl.syntax.exception.SyntaxNodeException;
import org.openl.syntax.impl.IdentifierNode;
import org.openl.syntax.impl.Tokenizer;
import org.openl.types.IOpenClass;
import org.openl.types.java.JavaOpenClass;

@Slf4j
public class DataTableMetaInfoReader extends BaseMetaInfoReader<DataTableBoundNode> {

    public DataTableMetaInfoReader(DataTableBoundNode boundNode) {
        super(boundNode);
    }

    @Override
    protected TableSyntaxNode getTableSyntaxNode() {
        return getBoundNode().getTableSyntaxNode();
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

        for (var i = 0; i < table.getNumberOfColumns(); i++) {
            var cell = data.getCell(i, 0);
            var logicalColStart = cell.getColumn();
            var logicalWidth = data.getColumnWidth(i);

            if (logicalColStart <= logicalCol && logicalCol < logicalColStart + logicalWidth) {
                // Found needed column for cell
                var descriptor = table.getColumnDescriptor(i);
                if (descriptor == null) {
                    continue;
                }
                IOpenClass columnType;
                if (descriptor instanceof ForeignKeyColumnDescriptor columnDescriptor) {
                    var db = getBoundNode().getDataBase();
                    columnType = columnDescriptor.getDomainClassForForeignTable(db);
                } else {
                    columnType = descriptor.isConstructor() ? table.getDataModel().getType() : descriptor.getType();
                }
                if (columnType == null) {
                    return null;
                }
                if (!descriptor.isValuesAnArray()) {
                    return new CellMetaInfo(columnType, false);
                } else {
                    if (descriptor instanceof ForeignKeyColumnDescriptor) {
                        return new CellMetaInfo(columnType, logicalWidth == 1);
                    } else {
                        var elemType = columnType.getAggregateInfo().getComponentType(columnType);
                        return new CellMetaInfo(elemType, logicalWidth == 1);
                    }
                }
            }
        }
        return null;
    }
}
