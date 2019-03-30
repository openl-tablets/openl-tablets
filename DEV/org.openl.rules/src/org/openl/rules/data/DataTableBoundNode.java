package org.openl.rules.data;

import org.openl.binding.IBindingContext;
import org.openl.binding.IMemberBoundNode;
import org.openl.binding.impl.module.ModuleOpenClass;
import org.openl.rules.lang.xls.binding.ATableBoundNode;
import org.openl.rules.lang.xls.binding.XlsModuleOpenClass;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.types.IOpenClass;
import org.openl.vm.IRuntimeEnv;

/**
 * @author snshor
 *
 */
public class DataTableBoundNode extends ATableBoundNode implements IMemberBoundNode {

    private DataOpenField field;
    private XlsModuleOpenClass module;
    private ITable table;

    public DataTableBoundNode(TableSyntaxNode tableSyntaxNode, XlsModuleOpenClass module) {
        super(tableSyntaxNode);
        this.module = module;
    }

    public DataOpenField getField() {
        return field;
    }

    public ITable getTable() {
        return table;
    }

    @Override
    public IOpenClass getType() {
        return field.getType();
    }

    public void setTable(ITable table) {
        this.table = table;
    }

    @Override
    protected Object evaluateRuntime(IRuntimeEnv env) {
        return null;
    }

    @Override
    public void addTo(ModuleOpenClass openClass) {
        
        TableSyntaxNode tableSyntaxNode = getTableSyntaxNode();
      
        field = new DataOpenField(table, openClass);
        openClass.addField(field);
        tableSyntaxNode.setMember(field);
    }

    @Override
    public void finalizeBind(IBindingContext cxt) throws Exception {
        table.populate(module.getDataBase(), cxt);
    }

    @Override
    public void removeDebugInformation(IBindingContext cxt) throws Exception {
        field.setTable(null);
    }

    public IDataBase getDataBase() {
        return module.getDataBase();
    }
}
