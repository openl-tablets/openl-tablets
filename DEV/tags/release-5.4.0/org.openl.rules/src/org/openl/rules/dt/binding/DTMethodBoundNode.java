/*
 * Created on Oct 7, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.rules.dt.binding;

import org.openl.OpenL;
import org.openl.binding.BindingDependencies;
import org.openl.binding.IBindingContext;
import org.openl.binding.IBindingContextDelegator;
import org.openl.binding.impl.module.ModuleOpenClass;
import org.openl.rules.dt.DTLoader;
import org.openl.rules.dt.DecisionTable;
import org.openl.rules.lang.xls.binding.AMethodBasedNode;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.types.IOpenMethod;
import org.openl.types.IOpenMethodHeader;

/**
 * @author snshor
 * 
 */
public class DTMethodBoundNode extends AMethodBasedNode {

    public DTMethodBoundNode(TableSyntaxNode tableSyntaxNode, OpenL openl, IOpenMethodHeader header,
            ModuleOpenClass module) {

        super(tableSyntaxNode, openl, header, module);
    }

    @Override
    protected IOpenMethod createMethodShell() {
        return DecisionTable.createTable(header);
    }

    public void finalizeBind(IBindingContext bindingContext) throws Exception {
        new DTLoader().load(getTableSyntaxNode(), getDecisionTable(), openl, module,
                (IBindingContextDelegator) bindingContext);
    }

    @Override
    public void updateDependency(BindingDependencies dependencies) {
        getDecisionTable().updateDependency(dependencies);
    }

    private DecisionTable getDecisionTable() {
        return (DecisionTable) method;
    }

}
