package org.openl.binding.impl.module;

import org.openl.binding.IBindingContext;
import org.openl.binding.IBoundNode;
import org.openl.binding.impl.BindHelper;
import org.openl.syntax.ISyntaxNode;
import org.openl.syntax.impl.IdentifierNode;
import org.openl.types.IOpenClass;

/**
 * @author Marat Kamalov
 */
public class ParameterDeclarationNodeBinderWithContextParameterSupport extends ParameterDeclarationNodeBinder {

    @Override
    protected void validateMetaData(ISyntaxNode syntaxNode, IBindingContext bindingContext) {
        if (syntaxNode.getNumberOfChildren() == 2) {
            var identifierNode = (IdentifierNode) syntaxNode.getChild(0);
            if (!"context".equals(identifierNode.getText())) {
                BindHelper
                        .processError("Illegal context parameter declaration.", syntaxNode.getChild(0), bindingContext);
            }
        } else {
            BindHelper.processError("Illegal context parameter declaration.", syntaxNode, bindingContext);
        }
    }

    @Override
    protected IBoundNode makeParameterNode(ISyntaxNode node,
                                           String name,
                                           IOpenClass type,
                                           IBindingContext bindingContext) {
        if (node.getNumberOfChildren() > 2) {
            var syntaxNode = node.getChild(2);
            if (syntaxNode.getNumberOfChildren() == 2) {
                var contextPropertyIdentifierNode = (IdentifierNode) syntaxNode.getChild(1);
                var contextProperty = contextPropertyIdentifierNode.getText();
                String errorMessage = ContextPropertyBinderUtils
                        .validateContextProperty(contextProperty, type, bindingContext);
                if (errorMessage != null) {
                    contextProperty = null;
                    BindHelper.processError(errorMessage, contextPropertyIdentifierNode, bindingContext);
                }
                return new ParameterNode(node, name, type, contextProperty);
            }
        }
        return super.makeParameterNode(node, name, type, bindingContext);
    }
}
