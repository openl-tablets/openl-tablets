package org.openl.rules.table.properties;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

//import org.apache.commons.logging.Log;
//import org.apache.commons.logging.LogFactory;
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
import org.openl.rules.table.properties.inherit.InheritanceLevelChecker;
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
    
    //private final Log LOG = LogFactory.getLog(PropertiesLoader.class);
    
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
            
            InheritanceLevelChecker.checkPropertiesLevel(InheritanceLevel.TABLE, propertiesInstance
                        .getPropertiesDefinedInTable().keySet());
                        
            tsn.setTableProperties(propertiesInstance);
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
            InheritanceLevelChecker.checkPropertiesLevel(InheritanceLevel.CATEGORY, categoryProperties
                    .getPropertiesAll().keySet());
            tableProperties.setPropertiesAppliedForCategory(categoryProperties.getPropertiesAll());
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
            InheritanceLevelChecker.checkPropertiesLevel(InheritanceLevel.MODULE, moduleProperties
                    .getPropertiesAll().keySet());
            tableProperties.setPropertiesAppliedForModule(moduleProperties.getPropertiesAll());
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
