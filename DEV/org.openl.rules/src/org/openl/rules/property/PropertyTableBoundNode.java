package org.openl.rules.property;

import java.util.Map.Entry;

import lombok.Getter;
import lombok.Setter;

import org.openl.binding.IBindingContext;
import org.openl.binding.IMemberBoundNode;
import org.openl.binding.impl.module.ModuleOpenClass;
import org.openl.rules.lang.xls.binding.ATableBoundNode;
import org.openl.rules.lang.xls.binding.XlsModuleOpenClass;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.table.properties.ITableProperties;
import org.openl.rules.table.properties.TableProperties;
import org.openl.rules.table.properties.def.TablePropertyDefinitionUtils;
import org.openl.rules.table.properties.inherit.InheritanceLevel;
import org.openl.types.IOpenClass;
import org.openl.types.java.JavaOpenClass;
import org.openl.vm.IRuntimeEnv;

public class PropertyTableBoundNode extends ATableBoundNode implements IMemberBoundNode {

    private PropertiesOpenField field;
    @Getter
    @Setter
    private TableProperties propertiesInstance;
    @Getter
    @Setter
    private String tableName;

    public PropertyTableBoundNode(TableSyntaxNode syntaxNode) {
        super(syntaxNode);
    }

    @Override
    public void addTo(ModuleOpenClass openClass) {
        addTo((XlsModuleOpenClass) openClass);
    }

    protected void addTo(XlsModuleOpenClass openClass) {
        var tsn = getTableSyntaxNode();
        if (tableName != null) {
            field = new PropertiesOpenField(tableName, propertiesInstance, openClass);
            openClass.addField(field);
            tsn.setMember(field);
        }
        if (InheritanceLevel.GLOBAL.getDisplayName().equals(propertiesInstance.getScope())) {
            ITableProperties globalProperties = TablePropertyDefinitionUtils
                    .buildGlobalTableProperties(propertiesInstance.getAllProperties());
            openClass.addGlobalTableProperties(globalProperties);
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

    private static TableProperties getTablePropertiesForExecutionMode(ITableProperties properties) {
        if (properties != null) {
            var clonedProperties = new TableProperties();
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
