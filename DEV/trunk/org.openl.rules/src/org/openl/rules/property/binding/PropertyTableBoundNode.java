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

    public PropertyTableBoundNode(TableSyntaxNode syntaxNode, XlsModuleOpenClass module) {
        super(syntaxNode, IBoundNode.EMPTY);        
    }

    public void addTo(ModuleOpenClass openClass) {
        // TODO Create OpenField if properties should be accessible in runtime, otherwise do nothing        
        
    }
    
    public Object evaluateRuntime(IRuntimeEnv env) throws OpenLRuntimeException {
        // don`t need to ????
        return null;
    }

    public void finalizeBind(IBindingContext cxt) throws Exception {
        // don`t need to finalize anything
    }

    public IOpenClass getType() {
        // TODO return Properties class if properties should be accessible in runtime, otherwise null
        // TODO discover what for this method is required
        return null;
    }

    public static class PropertiesOpenField extends AOpenField {

        private TableProperties propertiesInstance;

        /**
         * @param name
         * @param type
         */
        public PropertiesOpenField(String name, TableProperties propertiesInstance) {
            super(name, JavaOpenClass.getOpenClass(propertiesInstance.getClass()));
            this.propertiesInstance = propertiesInstance;
        }

        /**
         *
         */

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
