package org.openl.rules.table.properties;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.lang.StringUtils;
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
import org.openl.rules.table.LogicalTable;
import org.openl.rules.table.properties.def.TablePropertyDefinition;
import org.openl.rules.table.properties.def.TablePropertyDefinitionUtils;
import org.openl.rules.table.properties.inherit.InheritanceLevel;
import org.openl.rules.table.properties.inherit.PropertiesChecker;
import org.openl.rules.table.properties.inherit.InvalidPropertyLevelException;
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
    
    private final Log LOG = LogFactory.getLog(PropertiesLoader.class);
    
    private static final String PROPERTIES_SECTION_NAME = "Properties_Section";
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
     * @throws Exception when there problems loading properties with data table mechanism.
     * @throws InvalidPropertyLevelException if any property can`t be defined on table level.
     */
    private void loadPropertiesAsDataTable(TableSyntaxNode tsn) throws Exception {
        String propertySectionName = PROPERTIES_SECTION_NAME + tsn.getUri();
        DataNodeBinder bb = new DataNodeBinder();
        ITable propertyTable = module.getDataBase().addNewTable(propertySectionName, null);
        IOpenClass propetiesClass = JavaOpenClass.getOpenClass(TableProperties.class);
        ILogicalTable propertiesSection = binder.getPropertiesTableSection(tsn.getTable());        
        
        if (propertiesSection != null) {
            propertiesSection = LogicalTable.logicalTable(propertiesSection);
            bb.processTable(module, propertyTable, propertiesSection, propertySectionName, propetiesClass, cxt, openl,
                    false);
            TableProperties propertiesInstance = ((TableProperties[])propertyTable.getDataArray())[0]; 
            
            propertiesInstance.setPropertiesSection(propertiesSection);   
            
            String tableType = tsn.getType();
            
            Set<String> propertyNamesToCheck = propertiesInstance.getPropertiesDefinedInTable().keySet();
            
            checkProperties(propertyNamesToCheck, tableType);
            
            propertiesInstance.setCurrentTableType(tableType);
            
            tsn.setTableProperties(propertiesInstance);
        }
    }
    
    /**
     * We need to check loaded properties that all values are appropriate for this table. If there is any problem an 
     * error should be thrown. Now we check 2 situations:<br>
     *              1) properties can be defined on TABLE level.<br>
     *              2) properties can be defined for current type of table.   
     * 
     * @param propertyNamesToCheck properties names that are physically defined in table.
     * @param tableType type of he table. Shows whether it is a decision table or a data or some other.
     * 
     * @throws TablePropertiesException if there is any problem in loaded properties.
     */
    private void checkProperties(Set<String> propertyNamesToCheck, String tableType) 
        throws TablePropertiesException {        
        
        checkPropertiesLevel(propertyNamesToCheck);
        
        checkPropertiesForTableType(propertyNamesToCheck, tableType);        
    }

    private void checkPropertiesLevel(Set<String> propertyNamesToCheck) throws InvalidPropertyLevelException {
        InheritanceLevel currentLevel = InheritanceLevel.TABLE;
        for (String propertyNameToCheck : propertyNamesToCheck) { 
            if (!PropertiesChecker.isPropertySuitableForLevel(currentLevel, propertyNameToCheck)) {
                String msg = String.format("Property with name [%s] can`t be defined on the [%s] level.", 
                        propertyNameToCheck, currentLevel.getDisplayName());                
                throw new InvalidPropertyLevelException(msg);
            }
        }
        
    }
    
    /**
     * Checks if properties can be defined for given type of table.
     * 
     * @param propertyNamesToCheck
     * @param tableType
     * 
     * @throws TablePropertiesException if there is any problem in loaded properties.
     */
    private void checkPropertiesForTableType(Set<String> propertyNamesToCheck, String tableType) 
        throws TablePropertiesException {
        
        for (String propertyNameToCheck : propertyNamesToCheck) {
            if (!PropertiesChecker.canSetPropertyForTableType(propertyNameToCheck, tableType)) {
                throw new TablePropertiesException(String
                      .format("Property [%s] can`t be defined in table of type [%s].", propertyNameToCheck
                              , tableType));
            }
        }
    }

    /**
     * Load to tsn category properties from context.
     * 
     * @param tsn Tsn to load properties.
     * @throws InvalidPropertyLevelException if any property can`t be defined on category level.
     */
    private void loadCategoryProperties(TableSyntaxNode tsn) throws InvalidPropertyLevelException {
        ITableProperties tableProperties = tsn.getTableProperties();        
        String category = getCategory(tsn); 
        TableSyntaxNode categoryPropertiesTsn = cxt
                .getTableSyntaxNode(RulesModuleBindingContext.CATEGORY_PROPERTIES_KEY + category);
        if (categoryPropertiesTsn != null) {
            ITableProperties categoryProperties = categoryPropertiesTsn.getTableProperties();
            
            tableProperties.setPropertiesAppliedForCategory(categoryProperties.getAllProperties());
            tableProperties.setCategoryPropertiesTable(categoryProperties.getPropertiesSection());
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
     * @throws InvalidPropertyLevelException if any property can`t be defined on module level. 
     */
    private void loadModuleProperties(TableSyntaxNode tsn) throws InvalidPropertyLevelException {
        ITableProperties tableProperties = tsn.getTableProperties();
        TableSyntaxNode modulePropertiesTsn = cxt.getTableSyntaxNode(RulesModuleBindingContext.MODULE_PROPERTIES_KEY);
        if (tableProperties != null && modulePropertiesTsn != null) {
            ITableProperties moduleProperties = modulePropertiesTsn.getTableProperties();
            
            tableProperties.setPropertiesAppliedForModule(moduleProperties.getAllProperties());
            tableProperties.setModulePropertiesTable(moduleProperties.getPropertiesSection());
        }
    }

    /**
     * Load to tsn default properties.
     * 
     * @param tsn Tsn to load properties.
     */
    private void loadDefaultProperties(TableSyntaxNode tsn) {
        ITableProperties properties = tsn.getTableProperties();
        List<TablePropertyDefinition> propertiesWithDefaultValues = TablePropertyDefinitionUtils
                                                                            .getPropertiesToBeSetByDefault();    
        Map<String, Object> defaultProperties = new HashMap<String, Object>();
        for(TablePropertyDefinition propertyWithDefaultValue : propertiesWithDefaultValues){            
            String defaultPropertyName = propertyWithDefaultValue.getName();            
            Class<?> defaultPropertyValueType = TablePropertyDefinitionUtils.getPropertyByName(defaultPropertyName).getType()
            .getInstanceClass();
            IString2DataConvertor converter = String2DataConvertorFactory.getConvertor(defaultPropertyValueType);
            Object defaultValue = converter.parse(propertyWithDefaultValue.getDefaultValue(),
                    propertyWithDefaultValue.getFormat(), null);
            defaultProperties.put(defaultPropertyName, defaultValue);
        }
        properties.setPropertiesAppliedByDefault(defaultProperties);
    }
    
    private void createTableProperties(TableSyntaxNode tsn) {       
        ITableProperties properties = tsn.getTableProperties();
        properties = new TableProperties();  
        properties.setCurrentTableType(tsn.getType());
        tsn.setTableProperties(properties);
    }
    
    public void loadProperties(TableSyntaxNode tsn) throws Exception {
        // don`t need to load properties for tables with type XLS_PROPERTIES,
        // it will be processed during its binding.
        // author: DLiauchuk
        if (!ITableNodeTypes.XLS_PROPERTIES.equals(tsn.getType())) {
            try {
                loadPropertiesAsDataTable(tsn);
                if (tsn.getTableProperties() == null) {
                    createTableProperties(tsn);
                }
            } catch(Exception ex) {                
                createTableProperties(tsn);
                throw ex;
            }            
            loadCategoryProperties(tsn);
            loadModuleProperties(tsn);
            loadDefaultProperties(tsn);        
        }
    }
}
