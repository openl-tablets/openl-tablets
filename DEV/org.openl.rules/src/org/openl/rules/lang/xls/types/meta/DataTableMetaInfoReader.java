package org.openl.rules.lang.xls.types.meta;

import java.util.Collections;

import org.openl.binding.impl.NodeType;
import org.openl.binding.impl.NodeUsage;
import org.openl.binding.impl.SimpleNodeUsage;
import org.openl.exception.OpenLCompilationException;
import org.openl.meta.IMetaInfo;
import org.openl.rules.binding.RuleRowHelper;
import org.openl.rules.data.*;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.lang.xls.types.CellMetaInfo;
import org.openl.rules.table.CellKey;
import org.openl.rules.table.ICell;
import org.openl.rules.table.ILogicalTable;
import org.openl.rules.table.openl.GridCellSourceCodeModule;
import org.openl.source.IOpenSourceCodeModule;
import org.openl.syntax.exception.SyntaxNodeException;
import org.openl.syntax.impl.IdentifierNode;
import org.openl.syntax.impl.Tokenizer;
import org.openl.types.IOpenClass;
import org.openl.types.java.JavaOpenClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DataTableMetaInfoReader extends BaseMetaInfoReader<DataTableBoundNode> {
    private final Logger log = LoggerFactory.getLogger(DataTableMetaInfoReader.class);

    public DataTableMetaInfoReader(DataTableBoundNode boundNode) {
        super(boundNode);
    }

    @Override
    protected TableSyntaxNode getTableSyntaxNode() {
        return getBoundNode().getTableSyntaxNode();
    }

    @Override
    protected CellMetaInfo getHeaderMetaInfo() {
        ILogicalTable table = getTableSyntaxNode().getTable();
        IOpenSourceCodeModule source = new GridCellSourceCodeModule(table.getSource(), null);

        DataTableBoundNode boundNode = getBoundNode();
        if (boundNode.getField() == null) {
            // Datatype contains errors
            return null;
        }

        IMetaInfo typeMeta = boundNode.getType().getMetaInfo();
        if (typeMeta != null) {
            try {
                IdentifierNode[] parsedHeader = Tokenizer.tokenize(source, " \n\r");
                return RuleRowHelper.createCellMetaInfo(
                        parsedHeader[DataNodeBinder.TYPE_INDEX],
                        typeMeta,
                        NodeType.DATATYPE
                );
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
            ITable table = getBoundNode().getTable();
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

    private boolean isDescription(ITable table, int row, int col) {
        if (table.getNumberOfRows() == 0) {
            // No data values in this table. Only description.
            return true;
        }
        ICell firstDataCell = table.getRowTable(0).getCell(0, 0);
        if (table.getData().isNormalOrientation()) {
            return row < firstDataCell.getAbsoluteRow();
        } else {
            return col < firstDataCell.getAbsoluteColumn();
        }
    }

    private CellMetaInfo getDescriptionMetaInfo(ITable table, int row, int col) {
        int numberOfColumns = table.getNumberOfColumns();
        for (int i = 0; i < numberOfColumns; i++) {
            ColumnDescriptor descriptor = table.getColumnDescriptor(i);
            CellMetaInfo metaInfo = checkForeignKeyInHeader(descriptor, row, col);
            if (metaInfo != NOT_FOUND) {
                return metaInfo;
            }
        }

        return null;
    }

    private CellMetaInfo checkForeignKeyInHeader(ColumnDescriptor descriptor, int row, int col) {
        if (descriptor instanceof ForeignKeyColumnDescriptor) {
            ForeignKeyColumnDescriptor foreignDescriptor = (ForeignKeyColumnDescriptor) descriptor;
            CellKey cellKey = foreignDescriptor.getForeignKeyCellCoordinate();
            if (isNeededCell(cellKey, row, col)) {
                // Found needed cell
                if (foreignDescriptor.isReference()) {
                    IDataBase db = getBoundNode().getDataBase();
                    IdentifierNode foreignKeyTable = foreignDescriptor.getForeignKeyTable();
                    ITable foreignTable = db.getTable(foreignKeyTable.getIdentifier());
                    if (foreignTable != null) {
                        NodeUsage nodeUsage = new SimpleNodeUsage(
                                foreignKeyTable,
                                foreignTable.getTableSyntaxNode().getHeaderLineValue().getValue(),
                                foreignTable.getTableSyntaxNode().getUri(),
                                NodeType.DATA);
                        return new CellMetaInfo(JavaOpenClass.STRING, false, Collections.singletonList(nodeUsage));
                    }

                }
                return null;
            }
        }

        return NOT_FOUND;
    }

    private CellMetaInfo getDataMetaInfo(ITable table, int row, int col) throws SyntaxNodeException {
        ILogicalTable data = table.getData();
        boolean normalOrientation = data.isNormalOrientation();

        ICell firstCell = table.getRowTable(0).getCell(0, 0);
        // logicalCol is column for normal orientation and is row for transposed table
        int logicalCol = normalOrientation ? col - firstCell.getAbsoluteColumn() : row - firstCell.getAbsoluteRow();

        for (int i = 0; i < table.getNumberOfColumns(); i++) {
            ICell cell = data.getCell(i, 0);
            int logicalColStart = cell.getColumn();
            int logicalWidth = data.getColumnWidth(i);

            if (logicalColStart <= logicalCol && logicalCol < logicalColStart + logicalWidth) {
                // Found needed column for cell
                ColumnDescriptor descriptor = table.getColumnDescriptor(i);
                if (descriptor == null) {
                    continue;
                }
                IOpenClass columnType;
                if (descriptor instanceof ForeignKeyColumnDescriptor) {
                    IDataBase db = getBoundNode().getDataBase();
                    columnType = ((ForeignKeyColumnDescriptor) descriptor).getDomainClassForForeignTable(db);
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
                        IOpenClass elemType = columnType.getAggregateInfo().getComponentType(columnType);
                        return new CellMetaInfo(elemType, logicalWidth == 1);
                    }
                }
            }
        }
        return null;
    }
}
