/*
 * Created on May 19, 2003 Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.binding.impl;

import org.openl.binding.IBindingContext;
import org.openl.binding.IBoundNode;
import org.openl.syntax.ISyntaxNode;
import org.openl.syntax.impl.ISyntaxConstants;
import org.openl.syntax.impl.IdentifierNode;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;

/**
 * @author snshor
 */

public class ChainBinder extends ANodeBinder {

    static class LongNameBuilder {

        ISyntaxNode node;
        IBindingContext bindingContext;
        int cnt = 0;
        String name = "";
        IBoundNode targetNode;

        LongNameBuilder(ISyntaxNode node, IBindingContext bindingContext) {
            this.node = node;
            this.bindingContext = bindingContext;
        }

        void bindName() {
            int n = node.getNumberOfChildren();

            for (; cnt < n; cnt++, name += '.') {
                ISyntaxNode child = node.getChild(cnt);
                if (child.getType().equals("identifier")) {
                    name = name + ((IdentifierNode) child).getIdentifier();
                } else {
                    return;
                }

                IOpenField field = bindingContext.findVar(ISyntaxConstants.THIS_NAMESPACE, name, true);
                // TODO merge syntax node
                if (field != null) {
                    ++cnt;
                    targetNode = new FieldBoundNode(child, field);
                    return;
                }

                IOpenClass type = bindingContext.findType(ISyntaxConstants.THIS_NAMESPACE, name);
                if (type != null) {
                    ++cnt;
                    targetNode = new TypeBoundNode(child, type);
                    return;
                }
            }
        }

    }

    /*
     * (non-Javadoc)
     * @see org.openl.binding.INodeBinder#bind(org.openl.parser.ISyntaxNode, org.openl.env.IOpenEnv,
     * org.openl.binding.IBindingContext)
     */
    public IBoundNode bind(ISyntaxNode node, IBindingContext bindingContext) throws Exception {

        LongNameBuilder builder = new LongNameBuilder(node, bindingContext);
        builder.bindName();
        int n = node.getNumberOfChildren();

        IBoundNode target = builder.targetNode;

        if (target == null) {

            if (builder.cnt > 0) {

                String message = "Can not resolve: " + builder.name;
                BindHelper.processError(message, node, bindingContext);

                return new ErrorBoundNode(node);

            } else {
                target = bindChildNode(node.getChild(0), bindingContext);
                builder.cnt = 1;
            }
        }

        // bind suffixes

        for (int i = builder.cnt; i < n; i++) {
            target = bindTargetNode(node.getChild(i), bindingContext, target);
        }

        return target;
    }

}
