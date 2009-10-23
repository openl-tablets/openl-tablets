/*
 * Created on Oct 3, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.rules.lang.xls.binding;

import java.util.ArrayList;
import java.util.Date;

import org.openl.OpenL;
import org.openl.binding.IBindingContext;
import org.openl.binding.IBoundNode;
import org.openl.binding.IMemberBoundNode;
import org.openl.binding.impl.ANodeBinder;
import org.openl.binding.impl.BoundError;
import org.openl.meta.ObjectValue;
import org.openl.meta.StringValue;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.lang.xls.utils.DoubleToDateCaster;
import org.openl.rules.lang.xls.utils.ICustomCaster;
import org.openl.rules.lang.xls.utils.IntegerToDateCaster;
import org.openl.rules.lang.xls.utils.StringToBooleanCaster;
import org.openl.rules.lang.xls.utils.StringToDateCaster;
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
                    if (isPropertyDefined(propertyName,row)) {
                        Class<?> propDefType = getPropertyDefinitionType(propertyName);
                        if(!canAutoCastPropertyValue(propertyValue, propDefType)) {                            
                            ICustomCaster customCaster = getCustomCaster(propertyValue, propDefType);
                            if(customCaster != null) {
                                propertyValue = customCaster.cast(propertyValue);
                            } else {
                                throwException(propertyName, propDefType, propertyValue, row);
                            }                            
                        }
                    }                    
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
    
    private void throwException(String propertyName, Class<?> propDefType,
            Object propertyValue, ILogicalTable row) throws BoundError {
        throw new BoundError(
              null,
              String
                      .format(
                              "Property \"%1s\" must be of type \"%2s\". "
                                      + "Found type is \"%3s\". Found value is \"%4s\".",
                              propertyName, propDefType.toString(),
                              propertyValue.getClass().toString(),
                              propertyValue), null,
              new GridTableSourceCodeModule(row.getGridTable()));        
    }
    
    /**     
     * @param propertyValue Property value from table.
     * @param propDefType Type of the property from {@link DefaultPropertyDefinitions}.
     * @return {@link ICustomCaster} that can cast property value to property definition type.
     */
    private ICustomCaster getCustomCaster(Object propertyValue,
            Class<?> propDefType) {
        ICustomCaster customCaster = null;
        if (propDefType.equals(Date.class) && propertyValue instanceof Integer) {
            customCaster = new IntegerToDateCaster();
        } else if (propDefType.equals(Date.class)
                && propertyValue instanceof Double) {
            customCaster = new DoubleToDateCaster();
        } else if (propDefType.equals(Boolean.class)
                && propertyValue instanceof String) {
            customCaster = new StringToBooleanCaster();
        } else if (propDefType.equals(Date.class)
                && propertyValue instanceof String) {
            customCaster = new StringToDateCaster();
        }
        return customCaster;
    }
    
    /**
     * Gets the property type drom {@link DefaultPropertyDefinitions} by
     * property name.
     * @param propertyName Property name from table.
     * @return
     */
    private Class<?> getPropertyDefinitionType(String propertyName) {
        Class<?> propDefType = Class.class;
        TablePropertyDefinition propDef = DefaultPropertyDefinitions
        .getPropertyByName(propertyName);
        propDefType = propDef.getType().getInstanceClass();
        return propDefType;
    }
    
    /**
     * 
     * @param propertyName Property name from table.
     * @param row Destination row to the property in table.
     * 
     * @return <code>True</code> if property with such name has been 
     * defined in {@link DefaultPropertyDefinitions}.
     * 
     * @throws BoundError If there is no property in {@link DefaultPropertyDefinitions}
     * with such name.
     */
    private boolean isPropertyDefined(String propertyName, ILogicalTable row) throws BoundError {
        boolean result = false;
        TablePropertyDefinition propDef = DefaultPropertyDefinitions.getPropertyByName(propertyName);
        if (propDef == null) {
            throw new BoundError(null, String.format(
                    "There is no property in definitions with name \"%1s\".",
                    propertyName), null, new GridTableSourceCodeModule(row
                    .getGridTable()));
        } else {
            result = true;
        }
        return result;
        
    }    

    /**
     * @param propertyValue Value of the property.
     * @param propDefType Type of the property from {@link DefaultPropertyDefinitions}. 
     *
     * @return <code>True</code> if type of property value from source can be cast to type 
     * of property in definitions by {@link Class#cast(Object)}. <code>False</code> if can`t.
     */
    private boolean canAutoCastPropertyValue(Object propertyValue, Class<?> propDefType) {
        boolean result = false;        
        try {
            propDefType.cast(propertyValue);
            result = true;
        } catch (ClassCastException e) {
            result = false;
        }
        return result;
    }
    
    public abstract IMemberBoundNode preBind(TableSyntaxNode syntaxNode, OpenL openl, IBindingContext cxt,
            XlsModuleOpenClass module) throws Exception;
}
