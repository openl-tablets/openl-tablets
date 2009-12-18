package org.openl.rules.property;

import org.openl.OpenL;
import org.openl.binding.IBindingContext;
import org.openl.binding.IMemberBoundNode;
import org.openl.rules.binding.DublicatedPropertiesTableException;
import org.openl.rules.binding.RulesModuleBindingContext;
import org.openl.rules.data.ITable;
import org.openl.rules.data.binding.DataNodeBinder;
import org.openl.rules.lang.xls.XlsSheetSourceCodeModule;
import org.openl.rules.lang.xls.XlsWorkbookSourceCodeModule;
import org.openl.rules.lang.xls.binding.ATableBoundNode;
import org.openl.rules.lang.xls.binding.XlsModuleOpenClass;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.property.binding.PropertyTableBoundNode;
import org.openl.rules.table.ILogicalTable;
import org.openl.rules.table.properties.TableProperties;
import org.openl.types.IOpenClass;
import org.openl.types.java.JavaOpenClass;

/**
 * @author DLiauchuk
 *
 */
public class PropertyTableBinder extends DataNodeBinder {

    @Override
    public IMemberBoundNode preBind(TableSyntaxNode tsn, OpenL openl,
            IBindingContext cxt, XlsModuleOpenClass module) throws Exception {
        assert cxt instanceof RulesModuleBindingContext;
        
        PropertyTableBoundNode propertyNode = (PropertyTableBoundNode) makeNode(tsn, module);

        String propTableName = "InheritedProperties: " + tsn.getUri();                
        
        ITable propertyTable = module.getDataBase().addNewTable(propTableName, tsn);
        
        IOpenClass propertiesClass = JavaOpenClass.getOpenClass(TableProperties.class);
        
        ILogicalTable  propTableBody = getTableBody(tsn);     
        
        processTable(module, propertyTable, propTableBody, propTableName, propertiesClass, cxt, openl, false);
                
        analysePropertiesNode(tsn, propertyTable, (RulesModuleBindingContext)cxt, propertyNode);

        return propertyNode;
    }
    
    private void analysePropertiesNode(TableSyntaxNode tsn, ITable propertyTable, RulesModuleBindingContext cxt, 
            PropertyTableBoundNode propertyNode) throws DublicatedPropertiesTableException {        
        TableProperties propertiesInstance = ((TableProperties[])propertyTable.getDataArray())[0]; 
        if (isModuleProperties(propertiesInstance)) {
            XlsWorkbookSourceCodeModule  module = ((XlsSheetSourceCodeModule)tsn.getModule()).getWorkbookSource();
            if (!cxt.isExistModuleProperties()) {
                cxt.setModuleProperties(propertiesInstance);
            } else {
                String moduleName = module.getDisplayName();
                throw new DublicatedPropertiesTableException(String.format("Properties for module %s already exists",
                        moduleName), propertyNode);
            }
        } else {
            String category = getCategoryToApplyProperties(tsn, propertiesInstance);
            if (!cxt.isExistCategoryProperties(category)){
                cxt.addCategoryProperties(category, propertiesInstance);
            } else {           
                throw new DublicatedPropertiesTableException(String.format("Properties for category %s already exists", 
                        category), propertyNode);
            }
        }
    }

    private String getCategoryToApplyProperties(TableSyntaxNode tsn, TableProperties properties) {
        String result = null;
        String category = properties.getCategory();
        if (category != null) {
            result = category; 
        } else {
            result = ((XlsSheetSourceCodeModule)tsn.getModule()).getSheetName();
        }
        return result;
    }
    
    private boolean isModuleProperties(TableProperties properties) {
        boolean result = false;
        String scope = properties.getScope();
        if (scope != null && "module".equals(scope)) {
            result = true;
        }
        return result;
    }

    protected ATableBoundNode makeNode(TableSyntaxNode tsn, XlsModuleOpenClass module) {
        return new PropertyTableBoundNode(tsn, module);
    }    
}
