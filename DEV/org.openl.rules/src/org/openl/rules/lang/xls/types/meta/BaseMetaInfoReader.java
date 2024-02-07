package org.openl.rules.lang.xls.types.meta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.openl.binding.IMemberBoundNode;
import org.openl.binding.MethodUtil;
import org.openl.binding.impl.NodeType;
import org.openl.binding.impl.NodeUsage;
import org.openl.binding.impl.SimpleNodeUsage;
import org.openl.rules.constants.ConstantOpenField;
import org.openl.rules.helpers.ArraySplitter;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.lang.xls.types.CellMetaInfo;
import org.openl.rules.table.CellKey;
import org.openl.rules.table.CompositeGrid;
import org.openl.rules.table.ICell;
import org.openl.rules.table.IGridRegion;
import org.openl.rules.table.IGridTable;
import org.openl.rules.table.ILogicalTable;
import org.openl.rules.table.properties.TableProperties;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.types.java.JavaOpenClass;

public abstract class BaseMetaInfoReader<T extends IMemberBoundNode> implements MetaInfoReader {
    protected static final CellMetaInfo NOT_FOUND = new CellMetaInfo(null, false);

    private static final Logger LOG = LoggerFactory.getLogger(BaseMetaInfoReader.class);
    private final Map<CellKey, Boolean> constantsMap = new HashMap<>();
    private final Set<ConstantOpenField> constantOpenFields = new HashSet<>();

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

    public void addConstant(ICell cell, ConstantOpenField constantOpenField) {
        int row = cell.getAbsoluteRow();
        int col = cell.getAbsoluteColumn();
        constantsMap.put(CellKey.CellKeyFactory.getCellKey(col, row), Boolean.TRUE);
        constantOpenFields.add(constantOpenField);
    }

    protected IGridTable getGridTable() {
        if (getTableSyntaxNode().getGridTable().getGrid() instanceof CompositeGrid) {
            return ((CompositeGrid) getTableSyntaxNode().getGridTable().getGrid()).getGridTables()[0];
        } else {
            return getTableSyntaxNode().getGridTable();
        }
    }

    private static SimpleNodeUsage createConstantNodeUsage(ConstantOpenField constantOpenField, int start, int end) {
        String description = MethodUtil.printType(constantOpenField.getType()) + " " + constantOpenField
                .getName() + " = " + constantOpenField.getValueAsString();
        return new SimpleNodeUsage(start,
                end,
                description,
                constantOpenField.getMemberMetaInfo().getSourceUrl(),
                NodeType.OTHER);
    }

    @Override
    public final CellMetaInfo getMetaInfo(int row, int col) {
        try {
            if (!IGridRegion.Tool.contains(getGridTable().getRegion(), col, row)) {
                return null;
            }

            if (isHeaderRow(row)) {
                // Header can be merged with several cells. First cell can contain meta info, others cannot.
                return isHeaderCell(row, col) ? getHeaderMetaInfo() : null;
            }

            if (isProperties(row, col)) {
                return getPropertiesMetaInfo(row, col);
            }

            CellMetaInfo cellMetaInfo = getBodyMetaInfo(row, col);

            if (Boolean.TRUE.equals(constantsMap.get(CellKey.CellKeyFactory.getCellKey(col, row)))) {
                ICell firstCell = getTableSyntaxNode().getTableBody().getSource().getCell(0, 0);
                int r = row - firstCell.getAbsoluteRow();
                int c = col - firstCell.getAbsoluteColumn();
                ICell theValueCell = getTableSyntaxNode().getTableBody().getSource().getCell(c, r);
                String[] tokens = ArraySplitter.split(theValueCell.getStringValue());
                String cellValue = theValueCell.getStringValue();
                int startFrom = 0;
                List<NodeUsage> nodeUsages = new ArrayList<>();
                for (String token : tokens) {
                    int start = cellValue.indexOf(token, startFrom);
                    startFrom = start + token.length();
                    for (ConstantOpenField constantOpenField : constantOpenFields) {
                        if (token.equals(constantOpenField.getName())) {
                            int end = start + constantOpenField.getName().length();
                            SimpleNodeUsage nodeUsage = createConstantNodeUsage(constantOpenField, start, end);
                            nodeUsages.add(nodeUsage);
                        }
                    }
                }
                if (!nodeUsages.isEmpty()) {
                    return cellMetaInfo != null ? new CellMetaInfo(cellMetaInfo.getDataType(),
                            cellMetaInfo.isMultiValue(),
                            nodeUsages) : new CellMetaInfo(JavaOpenClass.STRING, false, nodeUsages);
                }
            }
            return cellMetaInfo;
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
            return null;
        }
    }

    @Override
    public void prepare(IGridRegion region) {
        // By default, do nothing.
        // It can be inefficient for some tables to store meta info for all cells.
    }

    @Override
    public void release() {
        // By default, do nothing.
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
