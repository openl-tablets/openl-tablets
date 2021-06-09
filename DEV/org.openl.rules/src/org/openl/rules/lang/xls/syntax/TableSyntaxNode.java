/*
 * Created on Jun 16, 2004
 *
 * Developed by OpenRules Inc 2003-2004
 */

package org.openl.rules.lang.xls.syntax;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.Map;

import org.openl.meta.StringValue;
import org.openl.rules.annotations.Executable;
import org.openl.rules.lang.xls.XlsNodeTypes;
import org.openl.rules.lang.xls.XlsSheetSourceCodeModule;
import org.openl.rules.lang.xls.types.meta.EmptyMetaInfoReader;
import org.openl.rules.lang.xls.types.meta.MetaInfoReader;
import org.openl.rules.table.IGridTable;
import org.openl.rules.table.ILogicalTable;
import org.openl.rules.table.LogicalTableHelper;
import org.openl.rules.table.openl.GridCellSourceCodeModule;
import org.openl.rules.table.properties.ITableProperties;
import org.openl.rules.table.syntax.GridLocation;
import org.openl.rules.table.xls.XlsUrlParser;
import org.openl.syntax.impl.NaryNode;
import org.openl.types.IOpenMember;

/**
 * @author snshor
 */
public class TableSyntaxNode extends NaryNode {
    public static final TableSyntaxNode[] EMPTY_ARRAY = new TableSyntaxNode[0];

    private ILogicalTable table;

    private final HeaderSyntaxNode headerNode;

    private ITableProperties tableProperties;

    private IOpenMember member;

    private final Map<String, ILogicalTable> subTables = new HashMap<>();

    private Object validationResult;

    private MetaInfoReader metaInfoReader = EmptyMetaInfoReader.getInstance();

    private volatile String tableId;

    public TableSyntaxNode(String type,
            GridLocation pos,
            XlsSheetSourceCodeModule module,
            IGridTable gridTable,
            HeaderSyntaxNode header) {
        super(type, pos, null, module);
        table = LogicalTableHelper.logicalTable(gridTable);
        headerNode = header;
        header.setParent(this);
    }

    public void setTable(IGridTable gridTable) {
        table = LogicalTableHelper.logicalTable(gridTable);
    }

    public String getDisplayName() {
        return table.getSource().getCell(0, 0).getStringValue();
    }

    public GridLocation getGridLocation() {
        return (GridLocation) getLocation();
    }

    public HeaderSyntaxNode getHeader() {
        return headerNode;
    }

    public StringValue getHeaderLineValue() {
        String value = table.getSource().getCell(0, 0).getStringValue();

        if (value == null) {
            return new StringValue("");
        } else {
            return new StringValue(value, value, value, new GridCellSourceCodeModule(table.getSource(), 0, 0, null));
        }
    }

    public IOpenMember getMember() {
        return member;
    }

    public ITableProperties getTableProperties() {
        return tableProperties;
    }

    public Map<String, ILogicalTable> getSubTables() {
        return subTables;
    }

    public ILogicalTable getTable(String view) {
        return subTables.get(view);
    }

    public ILogicalTable getTable() {
        return table;
    }

    public IGridTable getGridTable() {
        return table.getSource();
    }

    /**
     * Gets the table body without header and properties section.
     *
     * @return table body, without header and properties section (if exists).
     */
    public ILogicalTable getTableBody() {
        int startRow = !hasPropertiesDefinedInTable() ? 1 : 2;

        if (table.getHeight() <= startRow) {
            return null;
        }
        return table.getRows(startRow);
    }

    public String getUri() {
        return getGridTable().getUri();
    }

    public XlsUrlParser getUriParser() {
        return getGridTable().getUriParser();
    }

    public String getId() {
        if (tableId == null) {
            synchronized (this) {
                if (tableId == null) {
                    tableId = TableUtils.makeTableId(getUri());
                }
            }
        }
        return tableId;
    }

    public Object getValidationResult() {
        return validationResult;
    }

    public XlsSheetSourceCodeModule getXlsSheetSourceCodeModule() {
        return (XlsSheetSourceCodeModule) getModule();
    }

    public void setMember(IOpenMember member) {
        this.member = member;
    }

    public void setTableProperties(ITableProperties properties) {
        tableProperties = properties;
    }

    public void setValidationResult(Object validationResult) {
        this.validationResult = validationResult;
    }

    /**
     * Checks if <code>{@link TableSyntaxNode}</code> has properties that were physically defined in appropriate table
     * in data source. <br>
     * Properties set by default are ignoring.
     *
     * @return <code>TRUE</code> if <code>{@link TableSyntaxNode}</code> has properties that were physically defined in
     *         appropriate table in data source.
     */
    public boolean hasPropertiesDefinedInTable() {
        boolean result = false;
        if (tableProperties != null && tableProperties.getPropertiesSection() != null) {
            result = true;
        }
        return result;
    }

    public boolean isExecutableNode() {
        if (getMember() != null) {
            Class<?> memberClass = getMember().getClass();
            Annotation[] annotations = memberClass.getAnnotations();

            for (Annotation annotation : annotations) {
                if (annotation instanceof Executable) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Use this method instead of {@link #getType()}. Returns the enum constant for the current node.
     *
     * @return the {@link XlsNodeTypes} for current TableSyntaxNode
     */
    public XlsNodeTypes getNodeType() {
        return XlsNodeTypes.getEnumByValue(getType());
    }

    public MetaInfoReader getMetaInfoReader() {
        return metaInfoReader;
    }

    public void setMetaInfoReader(MetaInfoReader metaInfoReader) {
        this.metaInfoReader = metaInfoReader;
    }
}
