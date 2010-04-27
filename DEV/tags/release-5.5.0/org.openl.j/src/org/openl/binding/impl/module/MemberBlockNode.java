package org.openl.binding.impl.module;

import org.openl.binding.IBindingContext;
import org.openl.binding.IBoundNode;
import org.openl.binding.IMemberBoundNode;
import org.openl.binding.impl.BlockNode;
import org.openl.syntax.ISyntaxNode;

public class MemberBlockNode extends BlockNode implements IMemberBoundNode {

    public MemberBlockNode(ISyntaxNode node, IBoundNode[] children) {
        super(node, children, 0);
    }

    /*
     * (non-Javadoc)
     * @see org.openl.binding.impl.module.IMemberBoundNode#addTo(org.openl.binding.impl.module.ModuleOpenClass)
     */
    public void addTo(ModuleOpenClass openClass) {
        for (int i = 0; i < children.length; i++) {
            ((IMemberBoundNode) children[i]).addTo(openClass);
        }
    }

    /*
     * (non-Javadoc)
     * @see org.openl.binding.impl.module.IMemberBoundNode#finalizeBind(org.openl.binding.IBindingContext)
     */
    public void finalizeBind(IBindingContext cxt) throws Exception {
    }

}
