/*
 * Created on Jun 14, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.binding.impl.module;

import org.openl.binding.IBindingContext;
import org.openl.binding.IBoundNode;
import org.openl.binding.impl.ANodeBinder;
import org.openl.syntax.ISyntaxNode;
import org.openl.types.IOpenClass;
import org.openl.util.TableNameChecker;

/**
 * @author snshor
 */
public class ParameterDeclarationNodeBinder extends ANodeBinder {

    protected void validateMetaData(ISyntaxNode syntaxNode, IBindingContext bindingContext) {
    }

    protected IBoundNode makeParameterNode(ISyntaxNode node,
                                           String name,
                                           IOpenClass type,
                                           IBindingContext bindingContext) {
        return new ParameterNode(node, name, type);
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openl.binding.INodeBinder#bind(org.openl.syntax.ISyntaxNode, org.openl.binding.IBindingContext)
     */
    @Override
    public final IBoundNode bind(ISyntaxNode node, IBindingContext bindingContext) throws Exception {
        IBoundNode typeNode = bindChildNode(node.getChild(0), bindingContext);
        IOpenClass type = typeNode.getType();
        ISyntaxNode child = node.getChild(1);
        String name = child.getText();
        if (node.getNumberOfChildren() > 2) {
            validateMetaData(node.getChild(2), bindingContext);
        }
        if (TableNameChecker.isInvalidJavaIdentifier(name)) {
            return makeErrorNode("Illegal parameter declaration.", node, bindingContext);
        }

        return makeParameterNode(node, name, type, bindingContext);

    }

}
