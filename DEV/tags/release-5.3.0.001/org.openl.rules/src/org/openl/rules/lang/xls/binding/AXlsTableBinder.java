/*
 * Created on Oct 3, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.rules.lang.xls.binding;

import java.util.ArrayList;

import org.openl.OpenL;
import org.openl.binding.IBindingContext;
import org.openl.binding.IBoundNode;
import org.openl.binding.IMemberBoundNode;
import org.openl.binding.impl.ANodeBinder;
import org.openl.binding.impl.BoundError;
import org.openl.meta.ObjectValue;
import org.openl.meta.StringValue;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.table.ILogicalTable;
import org.openl.rules.table.openl.GridTableSourceCodeModule;
import org.openl.rules.table.properties.DefaultPropertyDefinitions;
import org.openl.rules.table.properties.TablePropertyDefinition;
import org.openl.syntax.ISyntaxNode;

/**
 * @author snshor
 *
 */
public abstract class AXlsTableBinder extends ANodeBinder {

    static final public String PROPERTIES_HEADER = "properties";

    /*
     * (non-Javadoc)
     *
     * @see org.openl.binding.INodeBinder#bind(org.openl.syntax.ISyntaxNode,
     *      org.openl.binding.IBindingContext)
     */
    public IBoundNode bind(ISyntaxNode node, IBindingContext bindingContext) throws Exception {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * @param propTable
     */
    public TableProperties loadProperties(ILogicalTable table) throws Exception {

        if (table.getLogicalHeight() < 2) {
            return null;
        }

        ILogicalTable propTable = table.rows(1, 1);

        if (!PROPERTIES_HEADER.equals(propTable.getGridTable().getCell(0, 0).getStringValue())) {
            return null;
        }

        
        ILogicalTable propValues = propTable.columns(1);

        ArrayList<TableProperties.Property> properties = new ArrayList<TableProperties.Property>();

        int h = propValues.getLogicalHeight();
        for (int i = 0; i < h; i++) {
            ILogicalTable row = propValues.getLogicalRow(i);

            if (row.getLogicalWidth() < 2) {
                throw new BoundError(null, "Property table must have structure: [property_name] [property_value]",
                        null, new GridTableSourceCodeModule(row.getGridTable()));
            }

            String propertyName = row.getGridTable().getCell(0, 0).getStringValue();
            if (propertyName != null && propertyName.trim().length() != 0) {
                Object propertyValue = row.getLogicalColumn(1).getGridTable().getCell(0, 0).getObjectValue();
                if (propertyValue == null) {
                    continue;
                } else {
                    validateProperty(propertyName, propertyValue, row);
                }
    
                StringValue key = new StringValue(propertyName, "key", null, row.getGridTable().getUri(0, 0));
                ObjectValue value = new ObjectValue(propertyValue, "value", null, row.getLogicalColumn(1).getGridTable()
                        .getUri(0, 0));
                TableProperties.Property p = new TableProperties.Property(key, value);
                properties.add(p);
            }
        }

        return new TableProperties(propValues, properties.toArray(new TableProperties.Property[0]));

    }
    
    /**
     * Try to get the property definition by its given name from {@link DefaultPropertyDefinitions}.
     * If there is no such property throws an exception. If type of the property value can`t be
     * cast to its type from definition, also throws an error.
     * @param propertyName Name of the property.
     * @param propertyValue Value of the property.
     * @param row Row in table.
     * @throws BoundError
     */
    private void validateProperty(String propertyName, Object propertyValue,
            ILogicalTable row) throws BoundError {
        Class<?> propDefType = Class.class;
        TablePropertyDefinition propDef = DefaultPropertyDefinitions.getPropertyByName(propertyName);
        if (propDef == null) {
            throw new BoundError(null, String.format(
                    "There is no property in definitions with name \"%1s\".",
                    propertyName), null, new GridTableSourceCodeModule(row
                    .getGridTable()));
        } else {
            propDefType = propDef.getType().getInstanceClass();
            try {
                propDefType.cast(propertyValue);
            } catch (ClassCastException e) {
                throw new BoundError(null, String.format("Property \"%1s\" must be of type \"%2s\". " +
                		"Found type is \"%3s\". Found value is \"%4s\".", 
                		propertyName, propDefType.toString(), 
                		propertyValue.getClass().toString(), 
                		propertyValue),
                        null, new GridTableSourceCodeModule(row.getGridTable()));
            }
        }
    }
    
    public abstract IMemberBoundNode preBind(TableSyntaxNode syntaxNode, OpenL openl, IBindingContext cxt,
            XlsModuleOpenClass module) throws Exception;
}
