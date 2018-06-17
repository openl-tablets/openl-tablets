package org.openl.rules.datatype.binding;

import org.openl.binding.IBindingContext;
import org.openl.binding.IMemberBoundNode;
import org.openl.binding.impl.module.ModuleOpenClass;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.lang.xls.types.meta.AliasDatatypeMetaInfoReader;
import org.openl.types.impl.DomainOpenClass;
import org.openl.types.impl.InternalDatatypeClass;

/**
 * Class that represents bound node for alias data types of 'Datatype' table
 * component.
 */
public class AliasDatatypeBoundNode implements IMemberBoundNode {

	private TableSyntaxNode tableSyntaxNode;
	private DomainOpenClass domainOpenClass;
	private ModuleOpenClass moduleOpenClass;

	public AliasDatatypeBoundNode(TableSyntaxNode tableSyntaxNode, DomainOpenClass domain, ModuleOpenClass moduleOpenClass) {
		this.tableSyntaxNode = tableSyntaxNode;
		this.domainOpenClass = domain;
		this.moduleOpenClass = moduleOpenClass;
	}

	public void addTo(ModuleOpenClass openClass) {
		InternalDatatypeClass internalClassMember = new InternalDatatypeClass(domainOpenClass, openClass);
		tableSyntaxNode.setMember(internalClassMember);
	}

	public void finalizeBind(IBindingContext cxt) throws Exception {
        if (!cxt.isExecutionMode()) {
            tableSyntaxNode.setMetaInfoReader(new AliasDatatypeMetaInfoReader(this));
        }
		// Add new type to internal types of module.
		//
		moduleOpenClass.addType(domainOpenClass);
	}

    public void removeDebugInformation(IBindingContext cxt) {
        //nothing to remove
    }

	public TableSyntaxNode getTableSyntaxNode() {
		return tableSyntaxNode;
	}

	public DomainOpenClass getDomainOpenClass() {
		return domainOpenClass;
	}
}
