package org.openl.binding.impl;

import org.openl.binding.IBindingContext;
import org.openl.binding.IBoundNode;
import org.openl.source.IOpenSourceCodeModule;
import org.openl.source.impl.SubTextSourceCodeModule;
import org.openl.syntax.ISyntaxNode;
import org.openl.syntax.impl.ISyntaxConstants;
import org.openl.syntax.impl.IdentifierNode;
import org.openl.syntax.impl.Tokenizer;
import org.openl.types.IOpenField;
import org.openl.util.text.TextInfo;

public class RangeVariableBinder extends ANodeBinder {

    @Override
    public IBoundNode bind(ISyntaxNode node, IBindingContext bindingContext) throws Exception {

        String text = node.getModule().getCode();

        TextInfo info = new TextInfo(text);

        IOpenSourceCodeModule sourceModule = new SubTextSourceCodeModule(node.getModule(),
            node.getSourceLocation().getStart().getAbsolutePosition(info),
            node.getSourceLocation().getEnd().getAbsolutePosition(info) + 1);

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
