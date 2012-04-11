/*
 * Created on Oct 7, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.rules.data.binding;

import org.openl.binding.IBindingContext;
import org.openl.binding.IBoundNode;
import org.openl.binding.IMemberBoundNode;
import org.openl.binding.OpenLRuntimeException;
import org.openl.binding.impl.module.ModuleOpenClass;
import org.openl.rules.data.ITable;
import org.openl.rules.lang.xls.binding.ATableBoundNode;
import org.openl.rules.lang.xls.binding.XlsModuleOpenClass;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.types.IDynamicObject;
import org.openl.types.IOpenClass;
import org.openl.types.impl.AOpenField;
import org.openl.vm.IRuntimeEnv;

/**
 * @author snshor
 *
 */
public class DataTableBoundNode extends ATableBoundNode implements IMemberBoundNode {

    static public class DataOpenField extends AOpenField {
        ITable table;

        TableSyntaxNode tableSyntaxNode;

        // Object data;

        /**
         * @param name
         * @param type
         */
        public DataOpenField(ITable table, TableSyntaxNode tableSyntaxNode) {
            super(table.getDataModel().getName(), table.getDataModel().getType().getAggregateInfo()
                    .getIndexedAggregateType(table.getDataModel().getType(), 1));
            this.table = table;
            this.tableSyntaxNode = tableSyntaxNode;
        }

        /**
         *
         */

        public Object get(Object target, IRuntimeEnv env) {
            Object data = ((IDynamicObject) target).getFieldValue(name);

            if (data == null) {
                data = table.getDataArray();
                ((IDynamicObject) target).setFieldValue(name, data);
            }

            return data;
        }

        public ITable getTable() {
            return table;
        }

        public TableSyntaxNode getTableSyntaxNode() {
            return tableSyntaxNode;
        }

        /**
         *
         */

        @Override
        public boolean isWritable() {
            return true;
        }

        /**
         *
         */

        public void set(Object target, Object value, IRuntimeEnv env) {
            ((IDynamicObject) target).setFieldValue(name, value);
        }

    }

    DataOpenField field;

    XlsModuleOpenClass module;

    ITable table;

    /**
     * @param syntaxNode
     * @param children
     */
    public DataTableBoundNode(TableSyntaxNode dtNode, XlsModuleOpenClass module) {
        super(dtNode, IBoundNode.EMPTY);
        this.module = module;
    }

    public void addTo(ModuleOpenClass openClass) {
        openClass.addField(field = new DataOpenField(table, getTableSyntaxNode()));
        getTableSyntaxNode().setMember(field);
    }

    public Object evaluateRuntime(IRuntimeEnv env) throws OpenLRuntimeException {
        // TODO Auto-generated method stub
        return null;
    }

    public void finalizeBind(IBindingContext cxt) throws Exception {
        // module.getDataBase().validate();
        table.populate(module.getDataBase(), cxt);
    }

    public DataOpenField getField() {
        return field;
    }

    public ITable getTable() {
        return table;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openl.binding.IBoundNode#getType()
     */
    public IOpenClass getType() {
        return field.getType();
    }

    void setTable(ITable table) {
        this.table = table;
    }

}
