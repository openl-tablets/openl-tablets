/*
 * Created on May 28, 2003 Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.binding.impl;

import org.openl.binding.IBindingContext;
import org.openl.binding.IBoundNode;
import org.openl.syntax.ISyntaxNode;
import org.openl.syntax.exception.SyntaxNodeException;

/**
 * @author snshor
 * 
 */
public class BlockBinder extends ANodeBinder {

    /*
     * (non-Javadoc)
     * 
     * @see org.openl.binding.INodeBinder#bind(org.openl.parser.ISyntaxNode, org.openl.env.IOpenEnv,
     * org.openl.binding.IBindingContext)
     */
    @Override
    public IBoundNode bind(ISyntaxNode node, IBindingContext bindingContext) {

        IBoundNode[] children = null;

        try {
            bindingContext.pushLocalVarContext();
            children = bindChildren(node, bindingContext);
        } catch (SyntaxNodeException error) {
            bindingContext.addError(error);
        } finally {
            bindingContext.popLocalVarContext();
        }

        return new BlockNode(node, bindingContext.getLocalVarFrameSize(), children);
    }

}
