package org.openl.binding.impl;

import org.openl.binding.IBindingContext;
import org.openl.binding.IBoundNode;
import org.openl.source.IOpenSourceCodeModule;
import org.openl.syntax.ISyntaxNode;
import org.openl.syntax.impl.ISyntaxConstants;
import org.openl.syntax.impl.IdentifierNode;
import org.openl.syntax.impl.Tokenizer;
import org.openl.types.IOpenField;

public class RangeVariableBinder extends ANodeBinder {

    /**
     * Binds a range variable from the provided syntax node.
     *
     * <p>This method retrieves the source code module from the given syntax node, tokenizes
     * its content using ":" as the delimiter expecting exactly two identifiers, and then
     * resolves a corresponding range field within the binding context. If the input does not
     * consist of exactly two tokens, an error node is returned; if a matching range is found,
     * a field-bound node is returned; otherwise, null is returned.
     *
     * @param node the syntax node containing the range expression
     * @return a field-bound node if a valid range is resolved, an error node if the range format
     *         is invalid, or null if no matching range field is found
     * @throws Exception if an error occurs during the binding process
     */
    @Override
    public IBoundNode bind(ISyntaxNode node, IBindingContext bindingContext) throws Exception {
        IOpenSourceCodeModule sourceModule = node.getSourceCodeModule();
        IdentifierNode[] rangeParts = Tokenizer.tokenize(sourceModule, ":");

        if (rangeParts.length != 2) {
            return makeErrorNode("Wrong Range format: " + sourceModule.getCode(), node, bindingContext);
        }

        IOpenField om = bindingContext
                .findRange(ISyntaxConstants.THIS_NAMESPACE, rangeParts[0].getIdentifier(), rangeParts[1].getIdentifier());

        if (om != null) {
            return new FieldBoundNode(node, om);
        }

        return null;
    }
}
