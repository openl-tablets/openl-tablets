package org.openl.binding.impl;

import org.openl.binding.IBindingContext;
import org.openl.binding.IBoundNode;
import org.openl.syntax.ISyntaxNode;
import org.openl.syntax.impl.ISyntaxConstants;
import org.openl.syntax.impl.IdentifierNode;
import org.openl.types.IOpenClass;

/**
 * A binder for Type declaration. Like
 *
 * <li>List</li>
 * <li>String[]</li>
 * <li>int[][][]</li>
 *
 * @author Yury Molchan
 *
 */
public class TypeBinder extends ANodeBinder {

    public IBoundNode bind(ISyntaxNode node, IBindingContext bindingContext) throws Exception {

        ISyntaxNode typeNode = node.getChild(0);

        try {
            IOpenClass varType = getType(typeNode, bindingContext);

            return new TypeBoundNode(node, varType);
        } catch (Exception e) {
            return makeErrorNode(e.getMessage(), node, bindingContext);
        }
    }

    private IOpenClass getType(ISyntaxNode node, IBindingContext bindingContext) throws ClassNotFoundException {
        if (node instanceof IdentifierNode) {
            String typeName = ((IdentifierNode) node).getIdentifier();
            IOpenClass varType = bindingContext.findType(ISyntaxConstants.THIS_NAMESPACE, typeName);
            if (varType == null) {
                String message = String
                    .format("Can't bind node: '%s'. Can't find type: '%s'.", node.getModule().getCode(), typeName);

                throw new ClassNotFoundException(message);
            }
            BindHelper.checkOnDeprecation(node, bindingContext, varType);
            return varType;
        }
        IOpenClass arrayType = getType(node.getChild(0), bindingContext);
        return arrayType != null ? arrayType.getAggregateInfo().getIndexedAggregateType(arrayType, 1) : null;
    }
}
