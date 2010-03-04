/*
 * Created on Jun 16, 2004
 *
 * Developed by OpenRules Inc 2003-2004
 */

package org.openl.rules.lang.xls.syntax;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.openl.meta.StringValue;
import org.openl.rules.indexer.IDocumentType;
import org.openl.rules.indexer.IIndexElement;
import org.openl.rules.lang.xls.XlsSheetSourceCodeModule;
import org.openl.rules.table.IGridTable;
import org.openl.rules.table.ILogicalTable;
import org.openl.rules.table.LogicalTable;
import org.openl.rules.table.properties.ITableProperties;
import org.openl.rules.table.syntax.GridLocation;
import org.openl.syntax.ISyntaxError;
import org.openl.types.IOpenMember;

/**
 * @author snshor
 */

public class TableSyntaxNode extends NodeWithProperties implements IIndexElement {

    private ILogicalTable table;
    // String header;

    private HeaderSyntaxNode headerNode;
    // PropertySyntaxNode propertyNode;
    
    private ITableProperties tableProperties;

    private IOpenMember member;

    private Map<String, ILogicalTable> subTables = new HashMap<String, ILogicalTable>();

    private ArrayList<ISyntaxError> errors;

    private Object validationResult;

    public TableSyntaxNode(String type, GridLocation pos, XlsSheetSourceCodeModule module, IGridTable gridtable,
            HeaderSyntaxNode header) {
        super(type,  pos, null, module);
        table = LogicalTable.logicalTable(gridtable);
        headerNode = header;
        header.setParent(this);
    }

    public void setTable(IGridTable gridTable) {
        table = LogicalTable.logicalTable(gridTable);
    }

    public void addError(ISyntaxError error) {
        if (errors == null) {
            errors = new ArrayList<ISyntaxError>();
        }
        errors.add(error);
    }

    public String getCategory() {
        return IDocumentType.WORKSHEET_TABLE.getCategory();
    }


    public String getDisplayName() {
        return table.getGridTable().getCell(0, 0).getStringValue();
    }

    public ISyntaxError[] getErrors() {
        return errors == null ? null : (ISyntaxError[]) errors.toArray(ISyntaxError.EMPTY);
    }

    // public IIndexElement getParent()
    // {
    // return (XlsSheetSourceCodeModule)getModule();
    // }

    public GridLocation getGridLocation() {
        return (GridLocation) getLocation();
    }

    /**
     * @return
     */
    public HeaderSyntaxNode getHeader() {
        return headerNode;
    }

    public StringValue getHeaderLineValue() {
        String value = table.getGridTable().getCell(0, 0).getStringValue();
        String uri = table.getGridTable().getUri(0, 0);
        return new StringValue(value, value, value, uri);
    }

    public String getIndexedText() {
        // return table.getGridTable().getStringValue(0, 0);
        return null;
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

    /**
     * @return
     */
    public ILogicalTable getTable() {
        return table;
    }

    public ILogicalTable getTableBody() {        
        int startRow = !hasPropertiesDefinedInTable() ? 1 : 2;

        if (table.getLogicalHeight() <= startRow) {
            return null;
        }
        ILogicalTable tableBody = table.rows(startRow);
        return tableBody;
    }

    public String getUri() {
        return table.getGridTable().getUri();
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
     * in data source. <br>Properties set by default are ignoring.
     * @return <code>TRUE</code> if <code>{@link TableSyntaxNode}</code> has properties that were physically defined 
     * in appropriate table in data source. 
     */
    public boolean hasPropertiesDefinedInTable() {
        boolean result = false;        
        if (tableProperties != null
                && tableProperties.getPropertiesSection() != null
                && tableProperties.getPropertiesDefinedInTable().size() > 0) {
            result = true;
        }
        return result;
    }

}
