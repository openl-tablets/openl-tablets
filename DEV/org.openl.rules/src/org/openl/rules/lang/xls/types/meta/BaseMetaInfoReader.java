package org.openl.rules.lang.xls.types.meta;

import java.util.*;

import org.openl.binding.IMemberBoundNode;
import org.openl.binding.impl.NodeUsage;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.lang.xls.types.CellMetaInfo;
import org.openl.rules.table.CellKey;
import org.openl.rules.table.ICell;
import org.openl.rules.table.IGridRegion;
import org.openl.rules.table.ILogicalTable;
import org.openl.rules.table.properties.TableProperties;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.types.java.JavaOpenClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class BaseMetaInfoReader<T extends IMemberBoundNode> implements MetaInfoReader {
    protected static final CellMetaInfo NOT_FOUND = new CellMetaInfo(null, false);

    private final Logger log = LoggerFactory.getLogger(BaseMetaInfoReader.class);
    private final Map<CellKey, List<NodeUsage>> constantsMap = new HashMap<>();

    private T boundNode;

    public BaseMetaInfoReader(T boundNode) {
        this.boundNode = boundNode;
    }

    public void setBoundNode(T boundNode) {
        this.boundNode = boundNode;
    }

    public T getBoundNode() {
        return boundNode;
    }

    public void addConstant(ICell cell, NodeUsage nodeUsage) {
        int row = cell.getAbsoluteRow();
        int col = cell.getAbsoluteColumn();
        List<NodeUsage> nodeUsages = constantsMap.computeIfAbsent(CellKey.CellKeyFactory.getCellKey(col, row),
            e -> new ArrayList<>());
        nodeUsages.add(nodeUsage);
    }

    @Override
    public final CellMetaInfo getMetaInfo(int row, int col) {
        try {
            if (!IGridRegion.Tool.contains(getTableSyntaxNode().getGridTable().getRegion(), col, row)) {
                return null;
            }

            List<NodeUsage> nodeUsages = constantsMap.get(CellKey.CellKeyFactory.getCellKey(col, row));
            if (nodeUsages != null) {
                return new CellMetaInfo(JavaOpenClass.STRING, false, nodeUsages);
            }

            if (isHeaderRow(row)) {
                // Header can be merged with several cells. First cell can contain meta info, others can't.
                return isHeaderCell(row, col) ? getHeaderMetaInfo() : null;
            }

            if (isProperties(row, col)) {
                return getPropertiesMetaInfo(row, col);
            }

            return getBodyMetaInfo(row, col);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return null;
        }
    }

    @Override
    public void prepare(IGridRegion region) {
        // By default do nothing.
        // It can be inefficient for some tables to store meta info for all cells.
    }

    @Override
    public void release() {
        // By default do nothing.
    }

    protected abstract TableSyntaxNode getTableSyntaxNode();

    protected abstract CellMetaInfo getHeaderMetaInfo();

    protected abstract CellMetaInfo getBodyMetaInfo(int row, int col);

    protected boolean isNeededCell(CellKey cellKey, int row, int col) {
        return cellKey.getColumn() == col && cellKey.getRow() == row;
    }

    protected boolean isNeededCell(ICell cell, int row, int col) {
        return cell.getAbsoluteColumn() == col && cell.getAbsoluteRow() == row;
    }

    private boolean isHeaderRow(int row) {
        TableSyntaxNode syntaxNode = getTableSyntaxNode();
        return syntaxNode.getTable().getCell(0, 0).getAbsoluteRow() == row;
    }

    private boolean isHeaderCell(int row, int col) {
        TableSyntaxNode syntaxNode = getTableSyntaxNode();
        return isNeededCell(syntaxNode.getTable().getCell(0, 0), row, col);
    }

    private boolean isProperties(int row, int col) {
        TableSyntaxNode tableSyntaxNode = getTableSyntaxNode();
        if (!tableSyntaxNode.hasPropertiesDefinedInTable()) {
            return false;
        }

        ILogicalTable propertiesSection = tableSyntaxNode.getTableProperties().getPropertiesSection();
        ICell firstCell = propertiesSection.getCell(0, 0);
        int r = row - firstCell.getAbsoluteRow();
        int c = col - firstCell.getAbsoluteColumn();

        // When c == -1 and r == 0 it's the "properties" keyword.
        return c >= -1 && r >= 0 && r < propertiesSection.getHeight() && c < propertiesSection.getWidth();
    }

    private CellMetaInfo getPropertiesMetaInfo(int row, int col) {
        ILogicalTable propertiesSection = getTableSyntaxNode().getTableProperties().getPropertiesSection();

        ICell firstCell = propertiesSection.getCell(0, 0);
        int r = row - firstCell.getAbsoluteRow();
        int c = col - firstCell.getAbsoluteColumn();

        if (c == 1) {
            // Create meta info for property value
            String fieldName = propertiesSection.getCell(0, r).getStringValue();
            IOpenField field = JavaOpenClass.getOpenClass(TableProperties.class).getField(fieldName);
            if (field != null) {
                IOpenClass type = field.getType();
                if (type.getAggregateInfo().isAggregate(type)) {
                    return new CellMetaInfo(type.getAggregateInfo().getComponentType(type), true);
                } else {
                    return new CellMetaInfo(type, false);
                }
            }

            return null;
        }

        return null;
    }
}
