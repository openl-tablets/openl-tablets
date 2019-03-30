package org.openl.binding.impl;

import org.openl.binding.IBindingContext;
import org.openl.binding.IBoundNode;
import org.openl.syntax.ISyntaxNode;
import org.openl.syntax.impl.IdentifierNode;
import org.openl.types.IOpenClass;
import org.openl.vm.IRuntimeEnv;

public class IndexParameterDeclarationBinder extends ANodeBinder {

    @Override
    public IBoundNode bind(ISyntaxNode node, IBindingContext bindingContext) throws Exception {

        ISyntaxNode nameSyntaxNode;
        ISyntaxNode typeSyntaxNode;
        IBoundNode typeBoundNode = null;

        if (node.getNumberOfChildren() == 1) {
            nameSyntaxNode = node.getChild(0);
        } else {
            typeSyntaxNode = node.getChild(0);
            typeBoundNode = bindChildNode(typeSyntaxNode, bindingContext);
            nameSyntaxNode = node.getChild(1);

        }
        String name = ((IdentifierNode) nameSyntaxNode).getIdentifier();
        return new IndexParameterNode(node, typeBoundNode, name);
    }

    public static class IndexParameterNode extends ABoundNode {
        private final String name;
        private final IBoundNode typeBoundNode;

        IndexParameterNode(ISyntaxNode syntaxNode, IBoundNode typeBoundNode, String name) {
            super(syntaxNode, typeBoundNode);
            this.name = name;
            this.typeBoundNode = typeBoundNode;
        }

        @Override
        protected Object evaluateRuntime(IRuntimeEnv env) {
            return null;
        }

        @Override
        public IOpenClass getType() {
            return typeBoundNode == null ? null : typeBoundNode.getType();
        }

        public String getName() {
            return name;
        }
    }
}
