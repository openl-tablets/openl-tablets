package org.openl.rules.property.binding;

import org.openl.binding.IBindingContext;
import org.openl.binding.IBoundNode;
import org.openl.binding.IMemberBoundNode;
import org.openl.binding.OpenLRuntimeException;
import org.openl.binding.impl.module.ModuleOpenClass;
import org.openl.rules.lang.xls.binding.ATableBoundNode;
import org.openl.rules.lang.xls.binding.XlsModuleOpenClass;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.table.properties.TableProperties;
import org.openl.types.IDynamicObject;
import org.openl.types.IOpenClass;
import org.openl.types.impl.AOpenField;
import org.openl.types.java.JavaOpenClass;
import org.openl.vm.IRuntimeEnv;

public class PropertyTableBoundNode extends ATableBoundNode implements IMemberBoundNode {
    
    private PropertiesOpenField field;
    private TableProperties propertiesInstance;
    private String tableName;
    
    public PropertyTableBoundNode(TableSyntaxNode syntaxNode, XlsModuleOpenClass module) {
        super(syntaxNode, IBoundNode.EMPTY);        
    }

    public void addTo(ModuleOpenClass openClass) {             
        TableSyntaxNode tsn = getTableSyntaxNode();
        if (tableName != null) {
            field = new PropertiesOpenField(tableName, propertiesInstance);
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
    
    public static class PropertiesOpenField extends AOpenField {

        private TableProperties propertiesInstance;
       
        public PropertiesOpenField(String name, TableProperties propertiesInstance) {
            super(name, JavaOpenClass.getOpenClass(propertiesInstance.getClass()));
            this.propertiesInstance = propertiesInstance;
        }

        public Object get(Object target, IRuntimeEnv env) {
            Object data = ((IDynamicObject) target).getFieldValue(getName());

            if (data == null) {
                data = propertiesInstance;
                ((IDynamicObject) target).setFieldValue(getName(), data);
            }

            return data;
        }

        @Override
        public boolean isWritable() {
            return true;
        }

        public void set(Object target, Object value, IRuntimeEnv env) {
            ((IDynamicObject) target).setFieldValue(getName(), value);
        }

    }
    
}
