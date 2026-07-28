package org.openl.rules.lang.xls.types.meta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

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
import org.openl.rules.table.properties.TableProperties;
import org.openl.types.java.JavaOpenClass;

@Slf4j
public abstract class BaseMetaInfoReader<T extends IMemberBoundNode> implements MetaInfoReader {
    protected static final CellMetaInfo NOT_FOUND = new CellMetaInfo(null, false);

    private final Map<CellKey, Boolean> constantsMap = new HashMap<>();
    private final Set<ConstantOpenField> constantOpenFields = new HashSet<>();

    @Getter
    @Setter
    private T boundNode;

    public BaseMetaInfoReader(T boundNode) {
        this.boundNode = boundNode;
    }

    public void addConstant(ICell cell, ConstantOpenField constantOpenField) {
        var row = cell.getAbsoluteRow();
        var col = cell.getAbsoluteColumn();
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
        var description = MethodUtil.printType(constantOpenField.getType()) + " " + constantOpenField
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

            var cellMetaInfo = getBodyMetaInfo(row, col);

            if (Boolean.TRUE.equals(constantsMap.get(CellKey.CellKeyFactory.getCellKey(col, row)))) {
                var firstCell = getTableSyntaxNode().getTableBody().getSource().getCell(0, 0);
                var r = row - firstCell.getAbsoluteRow();
                var c = col - firstCell.getAbsoluteColumn();
                var theValueCell = getTableSyntaxNode().getTableBody().getSource().getCell(c, r);
                String[] tokens = ArraySplitter.split(theValueCell.getStringValue());
                var cellValue = theValueCell.getStringValue();
                var startFrom = 0;
                var nodeUsages = new ArrayList<NodeUsage>();
                for (String token : tokens) {
                    var start = cellValue.indexOf(token, startFrom);
                    startFrom = start + token.length();
                    for (ConstantOpenField constantOpenField : constantOpenFields) {
                        if (token.equals(constantOpenField.getName())) {
                            var end = start + constantOpenField.getName().length();
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
            log.error(e.getMessage(), e);
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
        var syntaxNode = getTableSyntaxNode();
        return syntaxNode.getTable().getCell(0, 0).getAbsoluteRow() == row;
    }

    private boolean isHeaderCell(int row, int col) {
        var syntaxNode = getTableSyntaxNode();
        return isNeededCell(syntaxNode.getTable().getCell(0, 0), row, col);
    }

    private boolean isProperties(int row, int col) {
        var tableSyntaxNode = getTableSyntaxNode();
        if (!tableSyntaxNode.hasPropertiesDefinedInTable()) {
            return false;
        }

        var propertiesSection = tableSyntaxNode.getTableProperties().getPropertiesSection();
        var firstCell = propertiesSection.getCell(0, 0);
        var r = row - firstCell.getAbsoluteRow();
        var c = col - firstCell.getAbsoluteColumn();

        // When c == -1 and r == 0 it's the "properties" keyword.
        return c >= -1 && r >= 0 && r < propertiesSection.getHeight() && c < propertiesSection.getWidth();
    }

    private CellMetaInfo getPropertiesMetaInfo(int row, int col) {
        var propertiesSection = getTableSyntaxNode().getTableProperties().getPropertiesSection();

        var firstCell = propertiesSection.getCell(0, 0);
        var r = row - firstCell.getAbsoluteRow();
        var c = col - firstCell.getAbsoluteColumn();

        if (c == 1) {
            // Create meta info for property value
            var fieldName = propertiesSection.getCell(0, r).getStringValue();
            var field = JavaOpenClass.getOpenClass(TableProperties.class).getField(fieldName);
            if (field != null) {
                var type = field.getType();
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
