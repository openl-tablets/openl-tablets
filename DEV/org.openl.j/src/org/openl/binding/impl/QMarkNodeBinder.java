/*
 * Created on Jun 16, 2003 Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.binding.impl;

import org.openl.binding.IBindingContext;
import org.openl.binding.IBoundNode;
import org.openl.syntax.ISyntaxNode;
import org.openl.types.IOpenClass;

/**
 * @author snshor
 *
 */
public class QMarkNodeBinder extends ANodeBinder {

    public IBoundNode bind(ISyntaxNode node, IBindingContext bindingContext) throws Exception {

        IBoundNode[] children = new IBoundNode[3];
        children[0] = bindChildNode(node.getChild(0), bindingContext);

        IBoundNode conditionNode = children[0];

        IBoundNode checkConditionNode = BindHelper.checkConditionBoundNode(conditionNode, bindingContext);

        if (checkConditionNode != conditionNode)
            return checkConditionNode;

        children[1] = bindChildNode(node.getChild(1), bindingContext);
        children[2] = bindChildNode(node.getChild(2), bindingContext);
        IOpenClass type1 = children[1].getType();
        IOpenClass type2 = children[2].getType();

        CastToWiderType castToWiderType = CastToWiderType.create(bindingContext, type1, type2);

        IOpenClass type = castToWiderType.getWiderType();
        if (castToWiderType.getCast1() != null) {
            children[1] = new CastNode(null, children[1], castToWiderType.getCast1(), type);
        }
        if (castToWiderType.getCast2() != null) {
            children[2] = new CastNode(null, children[2], castToWiderType.getCast2(), type);
        }

        return new QMarkNode(node, children, type);
    }


}
