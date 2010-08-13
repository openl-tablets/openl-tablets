package org.openl.rules.datatype.binding;

import org.openl.OpenL;
import org.openl.binding.IBindingContext;
import org.openl.binding.IMemberBoundNode;
import org.openl.binding.impl.module.ModuleOpenClass;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.table.ILogicalTable;
import org.openl.types.impl.DomainOpenClass;
import org.openl.types.impl.InternalDatatypeClass;

public class AliasDatatypeBoundNode implements IMemberBoundNode {

	private TableSyntaxNode tableSyntaxNode;
	private DomainOpenClass domainOpenClass;
	private ILogicalTable table;
	private OpenL openl;

	public AliasDatatypeBoundNode(TableSyntaxNode tableSyntaxNode, DomainOpenClass domain, ILogicalTable table, OpenL openl) {
		this.tableSyntaxNode = tableSyntaxNode;
		this.domainOpenClass = domain;
		this.table = table;
		this.openl = openl;
	}

	public void addTo(ModuleOpenClass openClass) {
		InternalDatatypeClass internalClassMember = new InternalDatatypeClass(domainOpenClass, openClass);
        tableSyntaxNode.setMember(internalClassMember);
	}

	public void finalizeBind(IBindingContext cxt) throws Exception {
	}

}
