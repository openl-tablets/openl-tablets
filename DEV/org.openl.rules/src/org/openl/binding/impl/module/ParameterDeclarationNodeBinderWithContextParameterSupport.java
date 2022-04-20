package org.openl.binding.impl.module;

import org.openl.binding.IBindingContext;
import org.openl.binding.IBoundNode;
import org.openl.binding.impl.BindHelper;
import org.openl.syntax.ISyntaxNode;
import org.openl.syntax.impl.IdentifierNode;
import org.openl.types.IOpenClass;

/**
 * @author Marat Kamalov
 *
 */
public class ParameterDeclarationNodeBinderWithContextParameterSupport extends ParameterDeclarationNodeBinder {

    protected void validateMetaData(ISyntaxNode syntaxNode, IBindingContext bindingContext) {
        if (syntaxNode.getNumberOfChildren() == 2) {
            IdentifierNode identifierNode = (IdentifierNode) syntaxNode.getChild(0);
            if (!"context".equals(identifierNode.getText())) {
                BindHelper
                    .processError("Illegal context parameter declaration.", syntaxNode.getChild(0), bindingContext);
            }
        } else {
            BindHelper.processError("Illegal context parameter declaration.", syntaxNode, bindingContext);
        }
    }

    protected IBoundNode makeParameterNode(ISyntaxNode node,
            String name,
            IOpenClass type,
            IBindingContext bindingContext) {
        if (node.getNumberOfChildren() > 2) {
            ISyntaxNode syntaxNode = node.getChild(2);
            if (syntaxNode.getNumberOfChildren() == 2) {
                IdentifierNode contextPropertyIdentifierNode = (IdentifierNode) syntaxNode.getChild(1);
                String contextProperty = contextPropertyIdentifierNode.getText();
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
