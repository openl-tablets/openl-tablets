package org.openl.rules.property;

import org.openl.OpenL;
import org.openl.binding.IBindingContext;
import org.openl.binding.IMemberBoundNode;
import org.openl.binding.impl.BindHelper;
import org.openl.rules.binding.RulesModuleBindingContext;
import org.openl.rules.data.DataNodeBinder;
import org.openl.rules.data.ITable;
import org.openl.rules.lang.xls.XlsSheetSourceCodeModule;
import org.openl.rules.lang.xls.XlsWorkbookSourceCodeModule;
import org.openl.rules.lang.xls.binding.ATableBoundNode;
import org.openl.rules.lang.xls.binding.XlsModuleOpenClass;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.property.exception.DuplicatedPropertiesTableException;
import org.openl.rules.table.ILogicalTable;
import org.openl.rules.table.LogicalTable;
import org.openl.rules.table.openl.GridCellSourceCodeModule;
import org.openl.rules.table.properties.TableProperties;
import org.openl.rules.table.properties.inherit.InheritanceLevel;
import org.openl.rules.table.properties.inherit.PropertiesChecker;
import org.openl.source.IOpenSourceCodeModule;
import org.openl.syntax.impl.IdentifierNode;
import org.openl.syntax.impl.Tokenizer;
import org.openl.types.IOpenClass;
import org.openl.types.java.JavaOpenClass;

/**
 * Binder for property table.
 * 
 * @author DLiauchuk
 * 
 */
public class PropertyTableBinder extends DataNodeBinder {

    private static final String DEFAULT_TABLE_NAME_PREFIX = "InheritedProperties: ";
    private static final String SCOPE_PROPERTY_NAME = "scope";

    @Override
    public IMemberBoundNode preBind(TableSyntaxNode tsn, OpenL openl, IBindingContext cxt, XlsModuleOpenClass module) throws Exception {

        assert cxt instanceof RulesModuleBindingContext;

        PropertyTableBoundNode propertyNode = (PropertyTableBoundNode) makeNode(tsn, module);

        String tableName = parseHeader(tsn);
        propertyNode.setTableName(tableName);

        if (tableName == null) {
            tableName = DEFAULT_TABLE_NAME_PREFIX + tsn.getUri();
        }

        ITable propertyTable = module.getDataBase().addNewTable(tableName, tsn);
        IOpenClass propertiesClass = JavaOpenClass.getOpenClass(TableProperties.class);
        ILogicalTable propTableBody = getTableBody(tsn);

        processTable(module, propertyTable, propTableBody, tableName, propertiesClass, cxt, openl, false);

        TableProperties propertiesInstance = ((TableProperties[]) propertyTable.getDataArray())[0];
        propertiesInstance.setPropertiesSection(tsn.getTable());
        propertiesInstance.setCurrentTableType(tsn.getType());

        tsn.setTableProperties(propertiesInstance);

        analysePropertiesNode(tsn, propertiesInstance, (RulesModuleBindingContext) cxt, propertyNode);

        propertyNode.setPropertiesInstance(propertiesInstance);

        return propertyNode;
    }

    /**
     * Parses table header. Consider that second token is the name of the table. <br>
     * <b>e.g.: Properties [tableName].</b>
     * 
     * @param tsn <code>{@link TableSyntaxNode}</code>
     * @return table name if exists.
     * @throws Exception
     */
    private String parseHeader(TableSyntaxNode tsn) throws Exception {

        ILogicalTable table = LogicalTable.logicalTable(tsn.getTable());
        IOpenSourceCodeModule src = new GridCellSourceCodeModule(table.getGridTable());

        IdentifierNode[] parsedHeader = Tokenizer.tokenize(src, " \n\r");

        if (parsedHeader.length > 1) {
            return parsedHeader[1].getIdentifier();
        }

        return null;
    }

    /**
     * Checks if current property table is a module level property or a category
     * level. Adds it to <code>{@link RulesModuleBindingContext}</code>. <br>
     * If module level properties already exists, or there are properties for
     * the category with the same name throws an
     * <code>{@link DuplicatedPropertiesTableException}</code>.
     * 
     * @param tableSyntaxNode <code>{@link TableSyntaxNode}</code>.
     * @param propertiesInstance <code>{@link TableProperties}</code>.
     * @param bindingContext <code>{@link RulesModuleBindingContext}</code>.
     * @param propertyNode Bound node for current property table.
     * @throws DuplicatedPropertiesTableException if module level properties
     *             already exists, or there are properties for the category with
     *             the same name.
     */
    private void analysePropertiesNode(TableSyntaxNode tableSyntaxNode,
            TableProperties propertiesInstance,
            RulesModuleBindingContext bindingContext,
            PropertyTableBoundNode propertyNode) throws DuplicatedPropertiesTableException {

        String scope = propertiesInstance.getScope();

        if (scope != null) {
            if (isModuleProperties(scope)) {
                processModuleProperties(tableSyntaxNode, propertiesInstance, bindingContext, propertyNode);
            } else if (isCategoryProperties(scope)) {
                processCategoryProperties(tableSyntaxNode, propertiesInstance, bindingContext, propertyNode);
            } else {
                String message = String.format("Value of the property [%s] is neither [%s] or [%s]",
                    SCOPE_PROPERTY_NAME,
                    InheritanceLevel.MODULE.getDisplayName(),
                    InheritanceLevel.CATEGORY.getDisplayName());

                BindHelper.processError(message, tableSyntaxNode, bindingContext);
            }
        } else {
            String message = String.format("There is no property with name [%s] defined in Properties component. It is obligatory.",
                SCOPE_PROPERTY_NAME);

            BindHelper.processError(message, tableSyntaxNode, bindingContext);
        }
    }

    private void processCategoryProperties(TableSyntaxNode tableSyntaxNode,
            TableProperties propertiesInstance,
            RulesModuleBindingContext bindingContext,
            PropertyTableBoundNode propertyNode) throws DuplicatedPropertiesTableException {

        String category = getCategoryToApplyProperties(tableSyntaxNode, propertiesInstance);
        String key = RulesModuleBindingContext.CATEGORY_PROPERTIES_KEY + category;
        InheritanceLevel currentLevel = InheritanceLevel.CATEGORY;

        checkPropertiesLevel(currentLevel, propertiesInstance, tableSyntaxNode, bindingContext);

        if (!bindingContext.isTableSyntaxNodeExist(key)) {
            bindingContext.registerTableSyntaxNode(key, tableSyntaxNode);
        } else {
            String message = String.format("Properties for category %s already exists", category);

            throw new DuplicatedPropertiesTableException(message, null, tableSyntaxNode);
        }
    }

    private void checkPropertiesLevel(InheritanceLevel currentLevel,
            TableProperties propertiesInstance,
            TableSyntaxNode tableSyntaxNode,
            IBindingContext bindingContext) {

        for (String propertyNameToCheck : propertiesInstance.getAllProperties().keySet()) {

            if (!PropertiesChecker.isPropertySuitableForLevel(currentLevel, propertyNameToCheck)) {
                String message = String.format("Property with name [%s] can`t be defined on the [%s] level.",
                    propertyNameToCheck,
                    currentLevel.getDisplayName());

                BindHelper.processError(message, tableSyntaxNode, bindingContext);
            }
        }
    }

    private void processModuleProperties(TableSyntaxNode tableSyntaxNode,
            TableProperties propertiesInstance,
            RulesModuleBindingContext bindingContext,
            PropertyTableBoundNode propertyNode) throws DuplicatedPropertiesTableException {

        String key = RulesModuleBindingContext.MODULE_PROPERTIES_KEY;

        InheritanceLevel currentLevel = InheritanceLevel.MODULE;

        checkPropertiesLevel(currentLevel, propertiesInstance, tableSyntaxNode, bindingContext);

        if (!bindingContext.isTableSyntaxNodeExist(key)) {
            bindingContext.registerTableSyntaxNode(key, tableSyntaxNode);
        } else {
            XlsWorkbookSourceCodeModule module = ((XlsSheetSourceCodeModule) tableSyntaxNode.getModule()).getWorkbookSource();
            String moduleName = module.getDisplayName();
            String message = String.format("Properties for module %s already exists", moduleName);

            throw new DuplicatedPropertiesTableException(message, null, tableSyntaxNode);
        }
    }

    /**
     * Find out the name of the category to apply properties for.
     * 
     * @param tsn <code>{@link TableSyntaxNode}</code>
     * @param properties <code>{@link TableProperties}</code>
     * @return the name of the category to apply properties for.
     */
    private String getCategoryToApplyProperties(TableSyntaxNode tsn, TableProperties properties) {

        String category = properties.getCategory();

        if (category != null) {
            return category;
        } else {
            return ((XlsSheetSourceCodeModule) tsn.getModule()).getSheetName();
        }
    }

    private boolean isModuleProperties(String scope) {
        return InheritanceLevel.MODULE.getDisplayName().equals(scope);
    }

    private boolean isCategoryProperties(String scope) {
        return InheritanceLevel.CATEGORY.getDisplayName().equals(scope);
    }

    protected ATableBoundNode makeNode(TableSyntaxNode tsn, XlsModuleOpenClass module) {
        return new PropertyTableBoundNode(tsn, module);
    }

}
