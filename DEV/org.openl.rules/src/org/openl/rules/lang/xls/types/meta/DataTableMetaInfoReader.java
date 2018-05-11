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

        IMetaInfo typeMeta = getBoundNode().getType().getMetaInfo();
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

            ICell firstDataCell = table.getRowTable(0).getCell(0, 0);
            if (row < firstDataCell.getAbsoluteRow()) {
                return getHeaderMetaInfo(table, row, col);
            }

            if (table.getNumberOfRows() > 0 && table.getNumberOfColumns() > 0) {
                // Data exist
                return getDataMetaInfo(table, col);
            }

            return null;
        } catch (SyntaxNodeException e) {
            log.error(e.getMessage(), e);
            return null;
        }
    }

    private CellMetaInfo getHeaderMetaInfo(ITable table, int row, int col) {
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

    private CellMetaInfo getDataMetaInfo(ITable table, int col) throws SyntaxNodeException {
        ICell firstCell = table.getRowTable(0).getCell(0, 0);
        int startCol = firstCell.getAbsoluteColumn();
        int c = col - startCol;
        ILogicalTable data = table.getData();

        for (int i = 0; i < table.getNumberOfColumns(); i++) {
            ICell cell = data.getCell(i, 0);
            int column = cell.getColumn();
            int columnWidth = data.getColumnWidth(i);

            if (column <= c && c < column + columnWidth) {
                // Found needed column for cell
                ColumnDescriptor descriptor = table.getColumnDescriptor(i);
                IOpenClass columnType;
                if (descriptor instanceof ForeignKeyColumnDescriptor) {
                    IDataBase db = getBoundNode().getDataBase();
                    columnType = ((ForeignKeyColumnDescriptor) descriptor).getDomainClassForForeignTable(db);
                } else {
                    columnType = descriptor.isConstructor() ? table.getDataModel().getType() : descriptor.getType();
                }
                if (!descriptor.isValuesAnArray()) {
                    return new CellMetaInfo(columnType, false);
                } else {
                    if (descriptor instanceof ForeignKeyColumnDescriptor) {
                        return new CellMetaInfo(columnType, columnWidth == 1);
                    } else {
                        IOpenClass elemType = columnType.getAggregateInfo().getComponentType(columnType);
                        return new CellMetaInfo(elemType, columnWidth == 1);
                    }
                }
            }
        }
        return null;
    }
}
