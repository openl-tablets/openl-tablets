/*
 * Created on Oct 7, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.rules.dt;

import org.openl.OpenL;
import org.openl.binding.BindingDependencies;
import org.openl.binding.IBindingContext;
import org.openl.binding.IBindingContextDelegator;
import org.openl.binding.impl.module.ModuleOpenClass;
import org.openl.rules.lang.xls.binding.AMethodBasedNode;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.types.IOpenMethod;
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
    protected IOpenMethod createMethodShell() {
        return new DecisionTable(getHeader());
    }

    public void finalizeBind(IBindingContext bindingContext) throws Exception {
        new DecisionTableLoader().loadAndBind(getTableSyntaxNode(), getDecisionTable(), getOpenl(), getModule(), (IBindingContextDelegator) bindingContext);
        if (bindingContext.isExecutionMode()) {
            getDecisionTable().setTableSyntaxNode(null);
            getDecisionTable().getMethodProperties().setModulePropertiesTable(null);
            getDecisionTable().getMethodProperties().setCategoryPropertiesTable(null);
            getDecisionTable().getMethodProperties().setPropertiesSection(null);
        }
    }

    public final DecisionTable getDecisionTable() {
        return (DecisionTable) getMethod();
    }

    @Override
    public void updateDependency(BindingDependencies dependencies) {
        getDecisionTable().updateDependency(dependencies);
    }

}
