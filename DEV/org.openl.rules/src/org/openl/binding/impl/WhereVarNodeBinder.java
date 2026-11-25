/*
 * Created on Jun 14, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.binding.impl;

import org.openl.binding.IBindingContext;
import org.openl.binding.IBoundNode;
import org.openl.syntax.ISyntaxNode;

/**
 * @author snshor
 */
public class WhereVarNodeBinder extends ANodeBinder {

    @Override
    public IBoundNode bind(ISyntaxNode node, IBindingContext bindingContext) throws Exception {
        // The variable takes the type of the value it is given.
        return LocalVarBinder.createLocalVarDeclarationNode(node, null, null, bindingContext);
    }

}
