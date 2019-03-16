package org.openl.binding.impl;

import java.util.LinkedList;

import org.openl.binding.IBindingContext;
import org.openl.binding.IBoundNode;
import org.openl.syntax.ISyntaxNode;
import org.openl.types.IOpenClass;
import org.openl.types.java.JavaOpenClass;

/**
 * Bindes the following:
 *
 * <li>new int[] {1,2,3}</li>
 * <li>new int[][] {{7,2},{3}}</li>
 * <li>new int[3]</li>
 * <li>new int[3][]</li>
 * <li>new int[3][5]</li>
 *
 * @author Yury Molchan
 */
public class NewArrayNodeBinder extends ANodeBinder {

    public IBoundNode bind(ISyntaxNode node, IBindingContext bindingContext) throws Exception {

        assertCountOfChild("New array node must have 1 subnode", node, 1);
        ISyntaxNode child = node.getChild(0);

        if (child.getType().equals("new.array.initialized")) {
            // Bind new int[] {1,2,3}
            IOpenClass arrayType = getType(child.getChild(0), bindingContext);
            return bindTypeNode(child.getChild(1), bindingContext, arrayType);
        } else {
            // Bind new int[3][5][][]
            IOpenClass arrayType = getType(child, bindingContext);

            while (child.getType().equals("array.index.empty")) {
                // skip [][]
                child = child.getChild(0);
            }

            LinkedList<IBoundNode> dimensionsExpressions = new LinkedList<>();
            while (child.getType().equals("array.index.expression")) {
                // count [5][3] - dimensions are stored in revers order
                // TODO push array dimension and initialization expressions in BExGrammar directly.
                IBoundNode exprNode = bindTypeNode(child.getChild(1), bindingContext, JavaOpenClass.INT);
                dimensionsExpressions.push(exprNode);
                child = child.getChild(0);
            }

            return new ArrayBoundNode(node, dimensionsExpressions.toArray(IBoundNode.EMPTY), arrayType);
        }
    }
}
