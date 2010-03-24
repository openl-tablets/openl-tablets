/*
 * Created on Jun 16, 2003 Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.binding.impl;

import org.openl.binding.IBindingContext;
import org.openl.binding.IBoundNode;
import org.openl.syntax.ISyntaxNode;
import org.openl.types.java.JavaOpenClass;

/**
 * @author snshor
 * 
 */
public class QMarkNodeBinder extends ANodeBinder {

    public IBoundNode bind(ISyntaxNode node, IBindingContext bindingContext) throws Exception {

        IBoundNode[] children = new IBoundNode[3];
        children[0] = bindChildNode(node.getChild(0), bindingContext);
        
        IBoundNode conditionNode = children[0];
        
        if (conditionNode.getType() != JavaOpenClass.BOOLEAN) {
            BindHelper.processError("Condition must have boolean type", conditionNode.getSyntaxNode(), bindingContext);

            return new ErrorBoundNode(node);
        }

        children[1] = bindChildNode(node.getChild(1), bindingContext);
        children[2] = bindType(node.getChild(2), bindingContext, children[1].getType());

        return new QMarkNode(node, children);
    }

}
