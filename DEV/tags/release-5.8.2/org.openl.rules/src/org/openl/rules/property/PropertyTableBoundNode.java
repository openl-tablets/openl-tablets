package org.openl.rules.property;

import org.openl.binding.IBindingContext;
import org.openl.binding.IBoundNode;
import org.openl.binding.IMemberBoundNode;
import org.openl.binding.impl.module.ModuleOpenClass;
import org.openl.exception.OpenLRuntimeException;
import org.openl.rules.lang.xls.binding.ATableBoundNode;
import org.openl.rules.lang.xls.binding.XlsModuleOpenClass;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.property.runtime.PropertiesOpenField;
import org.openl.rules.table.properties.TableProperties;
import org.openl.types.IOpenClass;
import org.openl.types.java.JavaOpenClass;
import org.openl.vm.IRuntimeEnv;

public class PropertyTableBoundNode extends ATableBoundNode implements IMemberBoundNode {
    
    private PropertiesOpenField field;
    private TableProperties propertiesInstance;
    private String tableName;
    
    public PropertyTableBoundNode(TableSyntaxNode syntaxNode, XlsModuleOpenClass module) {
        super(syntaxNode, new IBoundNode[0]);        
    }

    public void addTo(ModuleOpenClass openClass) {             
        TableSyntaxNode tsn = getTableSyntaxNode();
        if (tableName != null) {
            field = new PropertiesOpenField(tableName, propertiesInstance, openClass);
            openClass.addField(field);
            tsn.setMember(field);   
        }
    }
    
    public Object evaluateRuntime(IRuntimeEnv env) throws OpenLRuntimeException {
        // don`t need to ????
        return null;
    }

    public void finalizeBind(IBindingContext cxt) throws Exception {
        // don`t need to finalize anything
    }

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

    public void removeDebugInformation(IBindingContext cxt) throws Exception {
        //nothing to remove
    }
    
}
