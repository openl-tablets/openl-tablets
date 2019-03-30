/*
 * Created on Jun 14, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.binding.impl;

import org.openl.binding.IBindingContext;
import org.openl.binding.IBoundNode;
import org.openl.syntax.ISyntaxNode;
import org.openl.syntax.impl.IdentifierNode;

/**
 * @author snshor
 *
 */
public class WhereVarNodeBinder extends ANodeBinder {

    @Override
    public IBoundNode bind(ISyntaxNode node, IBindingContext bindingContext) throws Exception {

        String name = ((IdentifierNode) node.getChild(0)).getIdentifier();

        return LocalVarBinder.createLocalVarDeclarationNode(node, name, node.getChild(1), null, bindingContext, true);
    }

}
