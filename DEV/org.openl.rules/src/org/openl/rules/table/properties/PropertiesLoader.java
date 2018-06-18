package org.openl.rules.table.properties;

import java.util.Map;
import java.util.Set;

import org.openl.OpenL;
import org.openl.message.OpenLMessagesUtils;
import org.openl.rules.binding.RulesModuleBindingContext;
import org.openl.rules.data.DataNodeBinder;
import org.openl.rules.data.ITable;
import org.openl.rules.lang.xls.XlsNodeTypes;
import org.openl.rules.lang.xls.XlsSheetSourceCodeModule;
import org.openl.rules.lang.xls.binding.XlsModuleOpenClass;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.table.ILogicalTable;
import org.openl.rules.table.properties.def.TablePropertyDefinitionUtils;
import org.openl.rules.table.properties.inherit.InheritanceLevel;
import org.openl.rules.table.properties.inherit.PropertiesChecker;
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

    public static final String EXTERNAL_MODULE_PROPERTIES_KEY = "external-module-properties";

    private static final String PROPERTIES_SECTION_NAME = "Properties_Section";

    private OpenL openl;
    private RulesModuleBindingContext bindingContext;
    private XlsModuleOpenClass module;

    public PropertiesLoader(OpenL openl, RulesModuleBindingContext cxt, XlsModuleOpenClass module) {
        this.openl = openl;
        this.bindingContext = cxt;
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

        ITable propertyTable = module.getDataBase().registerTable(propertySectionName, tableSyntaxNode);
        IOpenClass propetiesClass = JavaOpenClass.getOpenClass(TableProperties.class);
        ILogicalTable propertiesSection = PropertiesHelper.getPropertiesTableSection(tableSyntaxNode.getTable());

        if (propertiesSection != null) {
            dataNodeBinder.processTable(module,
                propertyTable,
                propertiesSection,
                propertySectionName,
                propetiesClass,
                bindingContext,
                openl,
                false);

            TableProperties propertiesInstance = ((TableProperties[]) propertyTable.getDataArray())[0];
            propertiesInstance.setPropertiesSection(propertiesSection);

            String tableType = tableSyntaxNode.getType();
            Set<String> propertyNamesToCheck = propertiesInstance.getPropertiesDefinedInTable().keySet();

            PropertiesChecker.checkProperties(bindingContext, propertyNamesToCheck, tableSyntaxNode, InheritanceLevel.TABLE);

            propertiesInstance.setCurrentTableType(tableType);

            tableSyntaxNode.setTableProperties(propertiesInstance);
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
        TableSyntaxNode categoryPropertiesTsn = bindingContext.getTableSyntaxNode(RulesModuleBindingContext.CATEGORY_PROPERTIES_KEY + category);

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
        TableSyntaxNode modulePropertiesTsn = bindingContext.getTableSyntaxNode(RulesModuleBindingContext.MODULE_PROPERTIES_KEY);

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
        Map<String, Object> defaultProperties = TablePropertyDefinitionUtils.getPropertiesMapToBeSetByDefault();
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

        ITableProperties tableProperties = tsn.getTableProperties();
        TableSyntaxNode modulePropertiesTsn = bindingContext.getTableSyntaxNode(RulesModuleBindingContext.MODULE_PROPERTIES_KEY);
        ITableProperties moduleProperties = null;
        if (tableProperties != null && modulePropertiesTsn != null) {
            moduleProperties = modulePropertiesTsn.getTableProperties();
        }

        Map<String, Object> externalParams = bindingContext.getExternalParams();

        if (externalParams != null && externalParams.containsKey(EXTERNAL_MODULE_PROPERTIES_KEY) && externalParams.get(EXTERNAL_MODULE_PROPERTIES_KEY) instanceof ITableProperties) {

            if (tsn.getTableProperties() == null) {
                createTableProperties(tsn);
            }

            ITableProperties properties = tsn.getTableProperties();
            ITableProperties externalProperties = (ITableProperties) externalParams.get(EXTERNAL_MODULE_PROPERTIES_KEY);
            if (moduleProperties != null) {
                for (String key : externalProperties.getAllProperties().keySet()) {
                    if (moduleProperties.getAllProperties().keySet().contains(key)) {
                        bindingContext.addMessage(OpenLMessagesUtils.newErrorMessage("Property '" + key + "' has already defined via external properties! Remove it from module properties."));
                    }
                }
            }

            properties.setExternalPropertiesAppliedForModule(externalProperties.getAllProperties());
        }
    }
}
