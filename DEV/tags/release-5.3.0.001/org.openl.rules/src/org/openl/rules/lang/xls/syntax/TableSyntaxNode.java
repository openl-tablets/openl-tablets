/*
 * Created on Jun 16, 2004
 *
 * Developed by OpenRules Inc 2003-2004
 */

package org.openl.rules.lang.xls.syntax;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.openl.meta.ObjectValue;
import org.openl.meta.StringValue;
import org.openl.rules.indexer.IDocumentType;
import org.openl.rules.indexer.IIndexElement;
import org.openl.rules.lang.xls.XlsSheetSourceCodeModule;
import org.openl.rules.lang.xls.binding.TableProperties;
import org.openl.rules.lang.xls.binding.TableProperties.Property;
import org.openl.rules.table.IGridTable;
import org.openl.rules.table.ILogicalTable;
import org.openl.rules.table.LogicalTable;
import org.openl.rules.table.properties.DefaultTableProperties;
import org.openl.rules.table.properties.ITableProperties;
import org.openl.rules.table.syntax.GridLocation;
import org.openl.syntax.ISyntaxError;
import org.openl.syntax.ISyntaxNode;
import org.openl.syntax.impl.ASyntaxNode;
import org.openl.types.IOpenMember;

/**
 * @author snshor
 */

public class TableSyntaxNode extends ASyntaxNode implements IIndexElement {

    ILogicalTable table;
    // String header;

    HeaderSyntaxNode headerNode;
    // PropertySyntaxNode propertyNode;

    TableProperties tableProperties;

    IOpenMember member;

    Map<String, ILogicalTable> subTables = new HashMap<String, ILogicalTable>();

    ArrayList<ISyntaxError> errors;

    Object validationResult;

    private TablePropertiesAdapter tablePropertiesAdapter = new TablePropertiesAdapter();

    public TableSyntaxNode(String type, GridLocation pos, XlsSheetSourceCodeModule module, IGridTable gridtable,
            HeaderSyntaxNode header) {
        super(type, pos, module);
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

    public ISyntaxNode getChild(int i) {
        // TODO Auto-generated method stub
        return null;
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

    public int getNumberOfChildren() {
        // TODO Auto-generated method stub
        return 0;
    }

    public Property getProperty(String name) {
        return tableProperties == null ? null : tableProperties.getProperty(name);
    }

    public ObjectValue getPropertyValue(String name) {
        if (tableProperties == null || tableProperties.getProperty(name) == null) {
            return null;
        }
        return tableProperties.getProperty(name).getValue();
    }

    public String getPropertValueAsString(String name) {
        return tableProperties == null ? null : tableProperties.getPropertyValueAsString(name);
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
        int startRow = tableProperties == null ? 1 : 2;

        if (table.getLogicalHeight() <= startRow) {
            return null;
        }
        ILogicalTable tableBody = table.rows(startRow);
        return tableBody;
    }

    public TableProperties getTableProperties() {
        return tableProperties;
    }

    public ITableProperties getTableProperties2() {

        return tablePropertiesAdapter.getITableProperties();
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

    public void setTableProperties(TableProperties properties) {
        tableProperties = properties;
        tablePropertiesAdapter.setTableProperties(properties);
    }

    public void setValidationResult(Object validationResult) {
        this.validationResult = validationResult;
    }

    /**
     * Internal adapter class that adapts {@link TableProperties} to
     * {@link ITableProperties} interface. Used as temporal solution. In future
     * versions {@link TableProperties} will be removed with
     * {@link ITableProperties}.
     * 
     * Note: This implementation of adapter stores internally the converted
     * values of objects.
     * 
     * @author Alexey Gamanovich
     * 
     */
    private class TablePropertiesAdapter {

        private TableProperties tableProperties;
        private ITableProperties iTableProperties;

        public TablePropertiesAdapter() {
        }

        public void setTableProperties(TableProperties tableProperties) {
            this.tableProperties = tableProperties;
            this.iTableProperties = convert(this.tableProperties);
        }

        public ITableProperties getITableProperties() {
            return iTableProperties;
        }

        private ITableProperties convert(TableProperties tableProperties) {
            
            if (tableProperties == null) {
                return null;
            }
            
            DefaultTableProperties properties = new DefaultTableProperties();

            for (TableProperties.Property tableProperty : tableProperties.getProperties()) {

                String key = tableProperty.getKey().getValue();
                Object value = tableProperty.getValue().getValue();

                properties.put(key, value);
            }

            return properties;
        }
    }
}
