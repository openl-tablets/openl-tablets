package org.openl.rules.dt;

import org.openl.OpenL;
import org.openl.binding.BindingDependencies;
import org.openl.binding.IBindingContext;
import org.openl.binding.impl.module.ModuleOpenClass;
import org.openl.rules.lang.xls.binding.AMethodBasedNode;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.lang.xls.types.meta.DecisionTableMetaInfoReader;
import org.openl.rules.method.ExecutableRulesMethod;
import org.openl.types.IOpenMethodHeader;

/**
 * @author snshor
 *
 */
public class DecisionTableBoundNode extends AMethodBasedNode {

    public DecisionTableBoundNode(TableSyntaxNode tableSyntaxNode,
            OpenL openl,
            IOpenMethodHeader header,
            ModuleOpenClass module) {

        super(tableSyntaxNode, openl, header, module);
    }

    @Override
    protected ExecutableRulesMethod createMethodShell() {
        return new DecisionTable(getHeader(), this);
    }

    @Override
    public void finalizeBind(IBindingContext bindingContext) throws Exception {
        if (!bindingContext.isExecutionMode()) {
            getTableSyntaxNode().setMetaInfoReader(new DecisionTableMetaInfoReader(this));
        }

        super.finalizeBind(bindingContext);
        new DecisionTableLoader()
            .loadAndBind(getTableSyntaxNode(), getDecisionTable(), getOpenl(), getModule(), bindingContext);
    }

    public final DecisionTable getDecisionTable() {
        return (DecisionTable) getMethod();
    }

    @Override
    public void updateDependency(BindingDependencies dependencies) {
        getDecisionTable().updateDependency(dependencies);
    }

}
