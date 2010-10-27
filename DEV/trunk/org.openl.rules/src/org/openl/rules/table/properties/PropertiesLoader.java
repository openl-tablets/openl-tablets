package org.openl.rules.table.properties;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.openl.OpenL;
import org.openl.binding.impl.BindHelper;
import org.openl.rules.binding.RulesModuleBindingContext;
import org.openl.rules.convertor.IString2DataConvertor;
import org.openl.rules.convertor.String2DataConvertorFactory;
import org.openl.rules.data.DataNodeBinder;
import org.openl.rules.data.ITable;
import org.openl.rules.lang.xls.ITableNodeTypes;
import org.openl.rules.lang.xls.XlsNodeTypes;
import org.openl.rules.lang.xls.XlsSheetSourceCodeModule;
import org.openl.rules.lang.xls.binding.XlsModuleOpenClass;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.table.ILogicalTable;
import org.openl.rules.table.properties.def.TablePropertyDefinition;
import org.openl.rules.table.properties.def.TablePropertyDefinitionUtils;
import org.openl.rules.table.properties.inherit.InheritanceLevel;
import org.openl.rules.table.properties.inherit.PropertiesChecker;
import org.openl.syntax.exception.SyntaxNodeException;
import org.openl.syntax.exception.SyntaxNodeExceptionUtils;
import org.openl.types.IOpenClass;
import org.openl.types.java.JavaOpenClass;

/**
 * Loads all kinds of properties to tsn. At first load all properties defined in
 * source table. Then load category, module and default properties.
 * 
 * @author DLiauchuk
 * 
 */
public class PropertiesLoader {

    private static final String EXTERNAL_MODULE_PROPERTIES_KEY = "external-module-properties";

    private static final String PROPERTIES_SECTION_NAME = "Properties_Section";

    private OpenL openl;
    private RulesModuleBindingContext cxt;
    private XlsModuleOpenClass module;

    public PropertiesLoader(OpenL openl, RulesModuleBindingContext cxt, XlsModuleOpenClass module) {
        this.openl = openl;
        this.cxt = cxt;
        this.module = module;
    }

    /**
     * Load properties from source table as data table.
     * 
     * @param tableSyntaxNode Tsn to load properties.
     * @throws Exception when there problems loading properties with data table
     *             mechanism.
     */
    private void loadPropertiesAsDataTable(TableSyntaxNode tableSyntaxNode) throws Exception {

        String propertySectionName = PROPERTIES_SECTION_NAME + tableSyntaxNode.getUri();
        DataNodeBinder dataNodeBinder = new DataNodeBinder();

        ITable propertyTable = module.getDataBase().addNewTable(propertySectionName, tableSyntaxNode);
        IOpenClass propetiesClass = JavaOpenClass.getOpenClass(TableProperties.class);
        ILogicalTable propertiesSection = PropertiesHelper.getPropertiesTableSection(tableSyntaxNode.getTable());

        if (propertiesSection != null) {            
            dataNodeBinder.processTable(module,
                propertyTable,
                propertiesSection,
                propertySectionName,
                propetiesClass,
                cxt,
                openl,
                false);

            TableProperties propertiesInstance = ((TableProperties[]) propertyTable.getDataArray())[0];
            propertiesInstance.setPropertiesSection(propertiesSection);

            String tableType = tableSyntaxNode.getType();
            Set<String> propertyNamesToCheck = propertiesInstance.getPropertiesDefinedInTable().keySet();

            checkProperties(propertyNamesToCheck, tableSyntaxNode);

            propertiesInstance.setCurrentTableType(tableType);

            tableSyntaxNode.setTableProperties(propertiesInstance);
        }
    }

    /**
     * We need to check loaded properties that all values are appropriate for
     * this table. If there is any problem an error should be thrown. Now we
     * check 2 situations:<br>
     * 1) properties can be defined on TABLE level.<br>
     * 2) properties can be defined for current type of table.
     * 
     * @param propertyNamesToCheck properties names that are physically defined
     *            in table.
     * @param tableType type of he table. Shows whether it is a decision table
     *            or a data or some other.
     */
    private void checkProperties(Set<String> propertyNamesToCheck, TableSyntaxNode tableSyntaxNode) {

        checkPropertiesLevel(propertyNamesToCheck, tableSyntaxNode);
        checkPropertiesForTableType(propertyNamesToCheck, tableSyntaxNode);
    }

    private void checkPropertiesLevel(Set<String> propertyNamesToCheck, TableSyntaxNode tableSyntaxNode) {

        InheritanceLevel currentLevel = InheritanceLevel.TABLE;

        for (String propertyNameToCheck : propertyNamesToCheck) {
            if (!PropertiesChecker.isPropertySuitableForLevel(currentLevel, propertyNameToCheck)) {

                String message = String.format("Property '%s' can`t be defined on the '%s' level",
                    propertyNameToCheck,
                    currentLevel.getDisplayName());

                SyntaxNodeException error = SyntaxNodeExceptionUtils.createError(message, tableSyntaxNode);
                tableSyntaxNode.addError(error);
                BindHelper.processError(error);
            }
        }
    }

    /**
     * Checks if properties can be defined for given type of table.
     * 
     * @param propertyNamesToCheck
     * @param tableType
     * 
     */
    private void checkPropertiesForTableType(Set<String> propertyNamesToCheck, TableSyntaxNode tableSyntaxNode) {

        String tableType = tableSyntaxNode.getType();

        for (String propertyNameToCheck : propertyNamesToCheck) {
            if (!PropertiesChecker.isPropertySuitableForTableType(propertyNameToCheck, tableType)) {
                String message = String.format("Property '%s' can`t be defined in table of type '%s'",
                    propertyNameToCheck,
                    tableType);

                SyntaxNodeException error = SyntaxNodeExceptionUtils.createError(message, tableSyntaxNode);
                tableSyntaxNode.addError(error);
                BindHelper.processError(error);
            }
        }
    }

    /**
     * Load to tsn category properties from context.
     * 
     * @param tableSyntaxNode Tsn to load properties.
     */
    private void loadCategoryProperties(TableSyntaxNode tableSyntaxNode) {

        ITableProperties tableProperties = tableSyntaxNode.getTableProperties();
        String category = getCategory(tableSyntaxNode);
        TableSyntaxNode categoryPropertiesTsn = cxt.getTableSyntaxNode(
                RulesModuleBindingContext.CATEGORY_PROPERTIES_KEY + category);

        if (categoryPropertiesTsn != null) {
            ITableProperties categoryProperties = categoryPropertiesTsn.getTableProperties();
            tableProperties.setPropertiesAppliedForCategory(categoryProperties.getAllProperties());
            tableProperties.setCategoryPropertiesTable(categoryProperties.getPropertiesSection());
        }
    }

    private String getCategory(TableSyntaxNode tsn) {

        ITableProperties tableProperties = tsn.getTableProperties();
        String category = tableProperties.getCategory();

        if (category != null) {
            return category;
        } else {
            return ((XlsSheetSourceCodeModule) tsn.getModule()).getSheetName();
        }
    }

    /**
     * Load to tsn module properties from context.
     * 
     * @param tableSyntaxNode Tsn to load properties.
     */
    private void loadModuleProperties(TableSyntaxNode tableSyntaxNode) {

        ITableProperties tableProperties = tableSyntaxNode.getTableProperties();
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
     * @param tableSyntaxNode Tsn to load properties.
     */
    public void loadDefaultProperties(TableSyntaxNode tableSyntaxNode) {

        if (tableSyntaxNode.getTableProperties() == null) {
            createTableProperties(tableSyntaxNode);
        }

        ITableProperties properties = tableSyntaxNode.getTableProperties();
        List<TablePropertyDefinition> propertiesWithDefaultValues = TablePropertyDefinitionUtils
            .getPropertiesToBeSetByDefault();
        Map<String, Object> defaultProperties = new HashMap<String, Object>();

        for (TablePropertyDefinition propertyWithDefaultValue : propertiesWithDefaultValues) {
            String defaultPropertyName = propertyWithDefaultValue.getName();
            TablePropertyDefinition propertyDefinition = TablePropertyDefinitionUtils
                .getPropertyByName(defaultPropertyName);
            Class<?> defaultPropertyValueType = propertyDefinition.getType().getInstanceClass();

            IString2DataConvertor converter = String2DataConvertorFactory.getConvertor(defaultPropertyValueType);
            Object defaultValue = converter.parse(propertyWithDefaultValue.getDefaultValue(),
                propertyWithDefaultValue.getFormat(),
                null);

            defaultProperties.put(defaultPropertyName, defaultValue);
        }

        properties.setPropertiesAppliedByDefault(defaultProperties);
    }

    private void createTableProperties(TableSyntaxNode tableSyntaxNode) {

        ITableProperties properties = new TableProperties();
        properties.setCurrentTableType(tableSyntaxNode.getType());

        tableSyntaxNode.setTableProperties(properties);
    }

    public void loadProperties(TableSyntaxNode tsn) throws Exception {
        // Don`t need to load properties for Properties tables,
        // it will be processed during its binding.
        // author: DLiauchuk
        final String tableType = tsn.getType();
        if (!XlsNodeTypes.XLS_PROPERTIES.toString().equals(tableType)) {
            try {
                loadPropertiesAsDataTable(tsn);

                if (tsn.getTableProperties() == null) {
                    createTableProperties(tsn);
                }
            } catch (Exception ex) {
                createTableProperties(tsn);
                throw ex;
            }

            loadExternalProperties(tsn);
            loadCategoryProperties(tsn);
            loadModuleProperties(tsn);
            loadDefaultProperties(tsn);
        }
    }
    
    private void loadExternalProperties(TableSyntaxNode tsn) {
        
        Map<String, Object> externalParams = cxt.getExternalParams();
        
        if (externalParams != null 
                && externalParams.get(EXTERNAL_MODULE_PROPERTIES_KEY) != null 
                && externalParams.get(EXTERNAL_MODULE_PROPERTIES_KEY) instanceof ITableProperties) {
            
            if (tsn.getTableProperties() == null) {
                createTableProperties(tsn);
            }

            ITableProperties properties = tsn.getTableProperties();
            ITableProperties externalProperties = (ITableProperties) externalParams.get(EXTERNAL_MODULE_PROPERTIES_KEY);
            properties.setExternalPropertiesAppliedForModule(externalProperties.getAllProperties());
        }
    }
}
