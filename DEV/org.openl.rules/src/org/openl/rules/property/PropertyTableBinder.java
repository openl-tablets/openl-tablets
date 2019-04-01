package org.openl.rules.property;

import org.openl.OpenL;
import org.openl.binding.IBindingContext;
import org.openl.binding.IMemberBoundNode;
import org.openl.rules.binding.RulesModuleBindingContext;
import org.openl.rules.data.DataNodeBinder;
import org.openl.rules.data.ITable;
import org.openl.rules.lang.xls.XlsSheetSourceCodeModule;
import org.openl.rules.lang.xls.XlsWorkbookSourceCodeModule;
import org.openl.rules.lang.xls.binding.ATableBoundNode;
import org.openl.rules.lang.xls.binding.XlsModuleOpenClass;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.lang.xls.types.meta.PropertyTableMetaInfoReader;
import org.openl.rules.property.exception.DuplicatedPropertiesTableException;
import org.openl.rules.table.ILogicalTable;
import org.openl.rules.table.properties.TableProperties;
import org.openl.rules.table.properties.inherit.InheritanceLevel;
import org.openl.rules.table.properties.inherit.PropertiesChecker;
import org.openl.source.IOpenSourceCodeModule;
import org.openl.syntax.exception.SyntaxNodeException;
import org.openl.syntax.exception.SyntaxNodeExceptionUtils;
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
    public IMemberBoundNode preBind(TableSyntaxNode tsn,
            OpenL openl,
            IBindingContext cxt,
            XlsModuleOpenClass module) throws Exception {

        assert cxt instanceof RulesModuleBindingContext;

        PropertyTableBoundNode propertyNode = (PropertyTableBoundNode) makeNode(tsn, module, cxt);

        String tableName = parseHeader(tsn);
        propertyNode.setTableName(tableName);

        if (tableName == null) {
            tableName = DEFAULT_TABLE_NAME_PREFIX + tsn.getUri();
        }

        ITable propertyTable = module.getDataBase().registerTable(tableName, tsn);
        IOpenClass propertiesClass = JavaOpenClass.getOpenClass(TableProperties.class);
        ILogicalTable propTableBody = getTableBody(tsn);

        processTable(module, propertyTable, propTableBody, tableName, propertiesClass, cxt, openl, false);

        TableProperties propertiesInstance = ((TableProperties[]) propertyTable.getDataArray())[0];
        propertiesInstance.setPropertiesSection(tsn.getTable().getRows(1)); // Skip header
        propertiesInstance.setCurrentTableType(tsn.getType());

        PropertiesChecker.checkProperties(cxt,
            propertiesInstance.getAllProperties().keySet(),
            tsn,
            InheritanceLevel.getEnumByValue(propertiesInstance.getPropertyValueAsString(SCOPE_PROPERTY_NAME)));

        tsn.setTableProperties(propertiesInstance);

        analysePropertiesNode(tsn, propertiesInstance, (RulesModuleBindingContext) cxt);

        propertyNode.setPropertiesInstance(propertiesInstance);

        return propertyNode;
    }

    /**
     * Parses table header. Consider that second token is the name of the table. <br>
     * <b>e.g.: Properties [tableName].</b>
     *
     * @param tsn <code>{@link TableSyntaxNode}</code>
     * @return table name if exists.
     */
    private String parseHeader(TableSyntaxNode tsn) throws Exception {
        IOpenSourceCodeModule src = tsn.getHeader().getModule();

        IdentifierNode[] parsedHeader = Tokenizer.tokenize(src, " \n\r");

        if (parsedHeader.length > 1) {
            return parsedHeader[1].getIdentifier();
        }

        return null;
    }

    /**
     * Checks if current property table is a module level property or a category level. Adds it to
     * <code>{@link RulesModuleBindingContext}</code>. <br>
     * If module level properties already exists, or there are properties for the category with the same name throws an
     * <code>{@link DuplicatedPropertiesTableException}</code>.
     *
     * @param tableSyntaxNode <code>{@link TableSyntaxNode}</code>.
     * @param propertiesInstance <code>{@link TableProperties}</code>.
     * @param bindingContext <code>{@link RulesModuleBindingContext}</code>.
     * @param propertyNode Bound node for current property table.
     * @throws DuplicatedPropertiesTableException if module level properties already exists, or there are properties for
     *             the category with the same name.
     */
    private void analysePropertiesNode(TableSyntaxNode tableSyntaxNode,
            TableProperties propertiesInstance,
            RulesModuleBindingContext bindingContext) throws SyntaxNodeException {

        String scope = propertiesInstance.getScope();

        if (scope != null) {
            if (isModuleProperties(scope)) {
                processModuleProperties(tableSyntaxNode, propertiesInstance, bindingContext);
            } else if (isCategoryProperties(scope)) {
                processCategoryProperties(tableSyntaxNode, propertiesInstance, bindingContext);
            } else {
                String message = String.format("Value of the property '%s' is neither '%s' or '%s'",
                    SCOPE_PROPERTY_NAME,
                    InheritanceLevel.MODULE.getDisplayName(),
                    InheritanceLevel.CATEGORY.getDisplayName());

                throw SyntaxNodeExceptionUtils.createError(message, tableSyntaxNode);
            }
        } else {
            String message = String.format("There is no obligatory property '%s'", SCOPE_PROPERTY_NAME);

            throw SyntaxNodeExceptionUtils.createError(message, tableSyntaxNode);
        }
    }

    private void processCategoryProperties(TableSyntaxNode tableSyntaxNode,
            TableProperties propertiesInstance,
            RulesModuleBindingContext bindingContext) throws SyntaxNodeException {

        String category = getCategoryToApplyProperties(tableSyntaxNode, propertiesInstance);
        String key = RulesModuleBindingContext.CATEGORY_PROPERTIES_KEY + category;
        InheritanceLevel currentLevel = InheritanceLevel.CATEGORY;

        checkPropertiesLevel(currentLevel, propertiesInstance, tableSyntaxNode);

        if (!bindingContext.isTableSyntaxNodeExist(key)) {
            bindingContext.registerTableSyntaxNode(key, tableSyntaxNode);
        } else {
            String message = String.format("Properties for category '%s' already exists", category);

            throw new DuplicatedPropertiesTableException(message, null, tableSyntaxNode);
        }
    }

    private void checkPropertiesLevel(InheritanceLevel currentLevel,
            TableProperties propertiesInstance,
            TableSyntaxNode tableSyntaxNode) throws SyntaxNodeException {

        for (String propertyNameToCheck : propertiesInstance.getAllProperties().keySet()) {

            if (!PropertiesChecker.isPropertySuitableForLevel(currentLevel, propertyNameToCheck)) {
                String message = String.format("Property '%s' can`t be defined on the '%s' level",
                    propertyNameToCheck,
                    currentLevel.getDisplayName());

                throw SyntaxNodeExceptionUtils.createError(message, tableSyntaxNode);
            }
        }
    }

    private void processModuleProperties(TableSyntaxNode tableSyntaxNode,
            TableProperties propertiesInstance,
            RulesModuleBindingContext bindingContext) throws SyntaxNodeException {

        String key = RulesModuleBindingContext.MODULE_PROPERTIES_KEY;

        InheritanceLevel currentLevel = InheritanceLevel.MODULE;

        checkPropertiesLevel(currentLevel, propertiesInstance, tableSyntaxNode);

        if (!bindingContext.isTableSyntaxNodeExist(key)) {
            bindingContext.registerTableSyntaxNode(key, tableSyntaxNode);
        } else {
            XlsWorkbookSourceCodeModule module = ((XlsSheetSourceCodeModule) tableSyntaxNode.getModule())
                .getWorkbookSource();
            String moduleName = module.getDisplayName();
            String message = String.format("Properties for module '%s' already exists", moduleName);

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

    @Override
    protected ATableBoundNode makeNode(TableSyntaxNode tsn, XlsModuleOpenClass module, IBindingContext bindingContext) {
        PropertyTableBoundNode boundNode = new PropertyTableBoundNode(tsn);

        if (!bindingContext.isExecutionMode()) {
            tsn.setMetaInfoReader(new PropertyTableMetaInfoReader(boundNode));
        }

        return boundNode;
    }

}
