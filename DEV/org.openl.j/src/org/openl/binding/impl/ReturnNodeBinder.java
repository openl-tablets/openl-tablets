/*
 * Created on Jul 28, 2003 Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.binding.impl;

import org.openl.binding.IBindingContext;
import org.openl.binding.IBoundNode;
import org.openl.syntax.ISyntaxNode;
import org.openl.types.IOpenClass;
import org.openl.types.NullOpenClass;
import org.openl.types.java.JavaOpenClass;

/**
 * @author snshor
 * 
 */
public class ReturnNodeBinder extends ANodeBinder {

    @Override
    public IBoundNode bind(ISyntaxNode node, IBindingContext bindingContext) throws Exception {

        IOpenClass returnType;
        IBoundNode exprNode = null;

        if (node.getNumberOfChildren() == 1) {

            returnType = bindingContext.getReturnType();

            if (returnType == NullOpenClass.the) {
                IBoundNode chNode = bindChildNode(node.getChild(0), bindingContext);
                bindingContext.setReturnType(returnType = chNode.getType());
            }

            exprNode = bindTypeNode(node.getChild(0), bindingContext, returnType);

        } else if (bindingContext.getReturnType() != JavaOpenClass.VOID) {
            return makeErrorNode("The method must return a value", node, bindingContext);
        }

        IBoundNode[] children = {};

        if (exprNode != null) {
            children = new IBoundNode[] { exprNode };
        }

        return new ReturnNode(node, children);
    }

}
