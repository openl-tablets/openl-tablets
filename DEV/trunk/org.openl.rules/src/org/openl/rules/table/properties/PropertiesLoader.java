package org.openl.rules.table.properties;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openl.OpenL;
import org.openl.rules.binding.RulesModuleBindingContext;
import org.openl.rules.data.IString2DataConvertor;
import org.openl.rules.data.ITable;
import org.openl.rules.data.String2DataConvertorFactory;
import org.openl.rules.data.binding.DataNodeBinder;
import org.openl.rules.lang.xls.ITableNodeTypes;
import org.openl.rules.lang.xls.XlsSheetSourceCodeModule;
import org.openl.rules.lang.xls.binding.AXlsTableBinder;
import org.openl.rules.lang.xls.binding.XlsModuleOpenClass;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.table.ILogicalTable;
import org.openl.types.IOpenClass;
import org.openl.types.java.JavaOpenClass;

/**
 * Loads all kinds of properties to tsn. 
 * At first load all properties defined in source table. Then load category, module and default properties.
 *  
 * @author DLiauchuk
 *
 */
public class PropertiesLoader {
        
    private OpenL openl;
    private RulesModuleBindingContext cxt;
    private XlsModuleOpenClass module; 
    private AXlsTableBinder binder;
    
    public PropertiesLoader(OpenL openl, RulesModuleBindingContext cxt, XlsModuleOpenClass module, 
            AXlsTableBinder binder) {    
        this.openl = openl;
        this.cxt = cxt;
        this.module = module;
        this.binder = binder;
    }    
    
    /**
     * Load properties from source table as data table.
     * 
     * @param tsn Tsn to load properties.
     * @throws Exception
     */
    private void loadPropertiesAsDataTable(TableSyntaxNode tsn) throws Exception {
        String propertySectionName = "Properties_Section" + tsn.getUri();
        DataNodeBinder bb = new DataNodeBinder();
        ITable propertyTable = module.getDataBase().addNewTable(propertySectionName, null);
        IOpenClass propetiesClass = JavaOpenClass.getOpenClass(TableProperties.class);
        ILogicalTable propertiesSection = binder.getPropertiesTableSection(tsn.getTable());
        if (propertiesSection != null) {
            bb.processTable(module, propertyTable, propertiesSection, propertySectionName, propetiesClass, cxt, openl,
                    false);
            TableProperties propertiesInstance = ((TableProperties[])propertyTable.getDataArray())[0]; 
            
            propertiesInstance.setPropertiesSection(propertiesSection);
            tsn.setTableProperties(propertiesInstance);
        }
    }
    
    /**
     * Load to tsn category properties from context.
     * 
     * @param tsn Tsn to load properties.
     */
    private void loadCategoryProperties(TableSyntaxNode tsn) {
        ITableProperties tableProperties = tsn.getTableProperties();        
        String category = getCategory(tsn);  
        ITableProperties categoryProperties = cxt.getCategotyProperties(category);
        if (categoryProperties != null) {
            tableProperties.setPropertiesAppliedForCategory(categoryProperties.getPropertiesAll());
        }
        
    }
    
    private String getCategory(TableSyntaxNode tsn) {
        String result = null;
        ITableProperties tableProperties = tsn.getTableProperties();
        String category = tableProperties.getCategory();
        if (category != null) {
            result = category;
        } else {
            result = ((XlsSheetSourceCodeModule) tsn.getModule()).getSheetName();
        }
        return result;
    }

    /**
     * Load to tsn module properties from context.
     * 
     * @param tsn Tsn to load properties.
     */
    private void loadModuleProperties(TableSyntaxNode tsn) {
        ITableProperties tableProperties = tsn.getTableProperties();
        ITableProperties moduleProperties = cxt.getModuleProperties();
        if (tableProperties != null) {
            if (moduleProperties != null) {
                tableProperties.setPropertiesAppliedForModule(moduleProperties.getPropertiesAll());
            }            
        }
    }
    
    /**
     * Load to tsn default properties.
     * 
     * @param tsn Tsn to load properties.
     */
    private void loadDefaultProperties(TableSyntaxNode tsn) {
        ITableProperties properties = tsn.getTableProperties();
        List<TablePropertyDefinition> propertiesWithDefaultValues = DefaultPropertyDefinitions
                                                                            .getPropertiesToBeSetByDefault();    
        Map<String, Object> defaultProperties = new HashMap<String, Object>();
        for(TablePropertyDefinition propertyWithDefaultValue : propertiesWithDefaultValues){            
            String propertyName = propertyWithDefaultValue.getName();            
            Class<?> defaultValueType = DefaultPropertyDefinitions.getPropertyByName(propertyName).getType()
            .getInstanceClass();
            IString2DataConvertor converter = String2DataConvertorFactory.getConvertor(defaultValueType);
            Object defaultValue = converter.parse(propertyWithDefaultValue.getDefaultValue(),
                    propertyWithDefaultValue.getFormat(), null);
            
            // check that there is no property value and only then apply default value
            // TODO: remove setting default property value. migrate to mechanism with maps.
//            if (properties.getPropertyValue(propertyName) == null) {
//                properties.setDefaultPropertyValue(propertyName, defaultValue);             
//            }
            defaultProperties.put(propertyName, defaultValue);
        }
        properties.setPropertiesToBeSetByDefault(defaultProperties);
    }
    
    private void createTableProperties(TableSyntaxNode tsn) {       
        ITableProperties properties = tsn.getTableProperties();
        properties = new TableProperties();            
        tsn.setTableProperties(properties);
    }


    public void loadProperties(TableSyntaxNode tsn) throws Exception {
        if (!ITableNodeTypes.XLS_PROPERTIES.equals(tsn.getType())) {
            loadPropertiesAsDataTable(tsn);
            if (tsn.getTableProperties() == null) {
                createTableProperties(tsn);
            }
            loadCategoryProperties(tsn);
            loadModuleProperties(tsn);
            loadDefaultProperties(tsn);        
        }
    }
}
