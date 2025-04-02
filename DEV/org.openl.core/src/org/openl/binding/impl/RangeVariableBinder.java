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
     * Binds a syntax node to a field range.
     *
     * <p>This method tokenizes the source module of the provided node using a colon (":") as a delimiter. If the resulting parts do not
     * constitute exactly two tokens, an error node is returned indicating an invalid range format. Otherwise, the method attempts a range
     * lookup in the current namespace using the identifiers from the tokens and the provided binding context. If a corresponding field is found,
     * a new field-bound node is returned; if no matching field exists, the method returns null.
     *
     * @param node the syntax node to be bound
     * @param bindingContext the context used for range lookup
     * @return a field-bound node if a matching field is found, an error node if the range format is invalid, or null otherwise
     * @throws Exception if an error occurs during binding
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
