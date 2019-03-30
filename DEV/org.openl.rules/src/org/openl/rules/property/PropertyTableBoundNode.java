package org.openl.rules.property;

import java.util.Map.Entry;

import org.openl.binding.IBindingContext;
import org.openl.binding.IMemberBoundNode;
import org.openl.binding.impl.module.ModuleOpenClass;
import org.openl.rules.lang.xls.binding.ATableBoundNode;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.table.properties.ITableProperties;
import org.openl.rules.table.properties.TableProperties;
import org.openl.types.IOpenClass;
import org.openl.types.java.JavaOpenClass;
import org.openl.vm.IRuntimeEnv;

public class PropertyTableBoundNode extends ATableBoundNode implements IMemberBoundNode {

    private PropertiesOpenField field;
    private TableProperties propertiesInstance;
    private String tableName;

    public PropertyTableBoundNode(TableSyntaxNode syntaxNode) {
        super(syntaxNode);
    }

    @Override
    public void addTo(ModuleOpenClass openClass) {
        TableSyntaxNode tsn = getTableSyntaxNode();
        if (tableName != null) {
            field = new PropertiesOpenField(tableName, propertiesInstance, openClass);
            openClass.addField(field);
            tsn.setMember(field);
        }
    }

    @Override
    protected Object evaluateRuntime(IRuntimeEnv env) {
        // don`t need to ????
        return null;
    }

    @Override
    public void finalizeBind(IBindingContext cxt) {
        // don`t need to finalize anything
    }

    @Override
    public IOpenClass getType() {
        return JavaOpenClass.getOpenClass(propertiesInstance.getClass());
    }

    public void setPropertiesInstance(TableProperties propertiesInstance) {
        this.propertiesInstance = propertiesInstance;
    }

    public TableProperties getPropertiesInstance() {
        return propertiesInstance;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public String getTableName() {
        return tableName;
    }

    private static TableProperties getTablePropertiesForExecutionMode(ITableProperties properties) {
        if (properties != null) {
            TableProperties clonedProperties = new TableProperties();
            for (Entry<String, Object> pair : properties.getAllProperties().entrySet()) {
                clonedProperties.setFieldValue(pair.getKey(), pair.getValue());
            }
            return clonedProperties;
        } else {
            return null;
        }
    }

    @Override
    public void removeDebugInformation(IBindingContext cxt) {
        if (cxt.isExecutionMode() && field != null) {
            field.setPropertiesInstance(getTablePropertiesForExecutionMode(propertiesInstance));
        }
    }

}
