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
public class DecisionTableMethodBoundNode extends AMethodBasedNode {

	/**
	 * @param syntaxNode
	 * @param children
	 */
	public DecisionTableMethodBoundNode(TableSyntaxNode dtNode, OpenL openl,
			IOpenMethodHeader header, ModuleOpenClass module) {
		super(dtNode, openl, header, module);
	}

	@Override
	protected IOpenMethod createMethodShell() {
		return DecisionTable.createTable(header);
	}

	public final DecisionTable getDecisionTable() {
		return (DecisionTable) method;
	}

	public void finalizeBind(IBindingContext cxt) throws Exception {

		new DTLoader().load(getTableSyntaxNode(), getDecisionTable(), openl,
				module, (IBindingContextDelegator) cxt);

	}

	public void updateDependency(BindingDependencies dependencies) {
		getDecisionTable().updateDependency(dependencies);
	}

}
