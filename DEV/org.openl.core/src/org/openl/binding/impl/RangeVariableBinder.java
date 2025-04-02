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
     * Binds a range variable expressed in the node's source module to its corresponding field.
     *
     * <p>
     * The method retrieves the source module from the given syntax node and tokenizes its content with ":" as
     * the delimiter, expecting exactly two parts. If the tokenization does not yield two identifiers, it returns
     * an error node describing the invalid range format. Otherwise, it looks up the corresponding field in the
     * current namespace of the binding context and returns a new bound node if the field is found; if not, it returns null.
     * </p>
     *
     * @param node the syntax node containing the range definition
     * @param bindingContext the context used to resolve the range and bind the corresponding field
     * @return a bound node for the range if valid; an error node for an invalid range format; or null if no matching field exists
     * @throws Exception if an unexpected error occurs during binding
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
