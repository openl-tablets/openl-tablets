/*
 * Created on May 19, 2003 Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.binding.impl;

import java.util.ArrayList;
import java.util.List;

import org.openl.binding.IBindingContext;
import org.openl.binding.IBoundNode;
import org.openl.syntax.ISyntaxNode;
import org.openl.syntax.impl.ISyntaxConstants;
import org.openl.syntax.impl.IdentifierNode;
import org.openl.types.IAggregateInfo;
import org.openl.types.IOpenClass;
import org.openl.types.java.JavaOpenClass;

/**
 * @author snshor
 */

public class NewArrayNodeBinder extends ANodeBinder {

    /*
     * (non-Javadoc)
     * @see org.openl.binding.INodeBinder#bind(org.openl.parser.ISyntaxNode, org.openl.env.IOpenEnv,
     * org.openl.binding.IBindingContext)
     */
    public IBoundNode bind(ISyntaxNode node, IBindingContext bindingContext) throws Exception {

        int childrenCount = node.getNumberOfChildren();

        if (childrenCount != 1) {
            return makeErrorNode("New array node must have 1 subnode", node, bindingContext);
        }

        ISyntaxNode indexChild = node.getChild(0);
        int dimension = 0;

        if(isArrayWithInitialization(node)){
            indexChild = indexChild.getChild(0);
            while (indexChild.getType().equals("type.index")) {
                dimension++;
                indexChild = indexChild.getChild(0);
            }
        }
        while (indexChild.getType().equals("array.index.empty")) {
            dimension++;
            indexChild = indexChild.getChild(0);
        }

        List<ISyntaxNode> expressions = new ArrayList<>();

        while (indexChild.getType().equals("array.index.expression")) {
            expressions.add(indexChild.getChild(1));
            indexChild = indexChild.getChild(0);
        }

        int exprsize = expressions.size();
        IBoundNode[] exprAry = new IBoundNode[exprsize];

        for (int i = 0; i < exprAry.length; i++) {
            // Array dimension expressions are pushed in reverse order, and, to handle them correctly, we should 
            // invert expressions array.
            // TODO push array dimension and initialization expressions in BExGrammar directly. 
            exprAry[exprsize - i - 1] = bindTypeNode(expressions.get(i), bindingContext, JavaOpenClass.INT);
        }

        ISyntaxNode typeNode = indexChild;
        String typeName = ((IdentifierNode) typeNode).getIdentifier();
        IOpenClass componentType = bindingContext.findType(ISyntaxConstants.THIS_NAMESPACE, typeName);

        if (componentType == null) {
            return makeErrorNode("Type '" + typeName + "' is not found", typeNode, bindingContext);
        }

        IAggregateInfo info = componentType.getAggregateInfo();
        IOpenClass arrayType = info.getIndexedAggregateType(componentType, dimension + exprsize);

        if (isArrayWithInitialization(node)) {
            return bindTypeNode(node.getChild(0).getChild(1), bindingContext, arrayType);
        } else {
            return new ArrayBoundNode(node, exprAry, dimension, arrayType, componentType);
        }
    }

    private boolean isArrayWithInitialization(ISyntaxNode node) {
        return node.getChild(0).getType().equals("new.array.initialized");
    }
}
