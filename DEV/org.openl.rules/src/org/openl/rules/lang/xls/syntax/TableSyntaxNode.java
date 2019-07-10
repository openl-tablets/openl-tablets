/*
 * Created on Jun 16, 2004
 *
 * Developed by OpenRules Inc 2003-2004
 */

package org.openl.rules.lang.xls.syntax;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.LinkedHashMap;
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
import org.openl.syntax.exception.CompositeSyntaxNodeException;
import org.openl.syntax.exception.SyntaxNodeException;
import org.openl.syntax.impl.NaryNode;
import org.openl.types.IOpenMember;
import org.openl.util.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author snshor
 */
public class TableSyntaxNode extends NaryNode {
    private final Logger log = LoggerFactory.getLogger(TableSyntaxNode.class);

    private ILogicalTable table;

    private HeaderSyntaxNode headerNode;

    private ITableProperties tableProperties;

    private IOpenMember member;

    private Map<String, ILogicalTable> subTables = new HashMap<>();

    /**
     * Map with errors in the order in which they were inserted
     */
    private LinkedHashMap<ErrorKey, SyntaxNodeException> errors;

    private Object validationResult;

    private MetaInfoReader metaInfoReader = EmptyMetaInfoReader.getInstance();

    public TableSyntaxNode(String type,
            GridLocation pos,
            XlsSheetSourceCodeModule module,
            IGridTable gridtable,
            HeaderSyntaxNode header) {
        super(type, pos, null, module);
        table = LogicalTableHelper.logicalTable(gridtable);
        headerNode = header;
        header.setParent(this);
    }

    public void setTable(IGridTable gridTable) {
        table = LogicalTableHelper.logicalTable(gridTable);
    }

    public void addError(SyntaxNodeException error) {
        if (errors == null) {
            errors = new LinkedHashMap<>();
        }
        ErrorKey key = new ErrorKey(error);
        if (errors.get(key) != null) {
            String message = error.getMessage();
            log.warn("Skip duplicated message: " + message, error);
            return;
        }
        errors.put(key, error);
    }

    public void addError(CompositeSyntaxNodeException error) {
        SyntaxNodeException[] errors = error.getErrors();
        for (SyntaxNodeException e : errors) {
            addError(e);
        }
    }

    public void clearErrors() {
        errors = null;
    }

    public String getDisplayName() {
        return table.getSource().getCell(0, 0).getStringValue();
    }

    public SyntaxNodeException[] getErrors() {
        return errors == null ? null : errors.values().toArray(new SyntaxNodeException[0]);
    }

    public boolean hasErrors() {
        return CollectionUtils.isNotEmpty(errors);
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
        return TableUtils.makeTableId(getUri());
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
