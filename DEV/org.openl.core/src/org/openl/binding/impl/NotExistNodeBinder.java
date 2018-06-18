package org.openl.binding.impl;

import org.openl.binding.IBindingContext;
import org.openl.binding.IBoundNode;
import org.openl.binding.INodeBinder;
import org.openl.syntax.ISyntaxNode;
import org.openl.types.IOpenClass;

public final class NotExistNodeBinder implements INodeBinder {
    public static INodeBinder the = new NotExistNodeBinder();

    private NotExistNodeBinder() {
        // Disable to instantiate outside of this class.
    }

    private static IBoundNode makeErrorNode(ISyntaxNode node, IBindingContext bindingContext) {
        String message = "DEV: Binder is not found for node '" + node.getType() + "'";
        return ANodeBinder.makeErrorNode(message, node, bindingContext);
    }

    @Override
    public IBoundNode bind(ISyntaxNode node, IBindingContext bindingContext) {
        return makeErrorNode(node, bindingContext);
    }

    @Override
    public IBoundNode bindTarget(ISyntaxNode node, IBindingContext bindingContext, IBoundNode targetNode) {
        return makeErrorNode(node, bindingContext);
    }

    @Override
    public IBoundNode bindType(ISyntaxNode node, IBindingContext bindingContext, IOpenClass type) {
        return makeErrorNode(node, bindingContext);
    }
}
