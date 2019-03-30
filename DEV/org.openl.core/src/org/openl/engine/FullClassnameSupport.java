package org.openl.engine;

import java.lang.reflect.Field;

import org.openl.binding.IBindingContext;
import org.openl.exception.OpenlNotCheckedException;
import org.openl.syntax.ISyntaxNode;
import org.openl.syntax.code.IParsedCode;
import org.openl.syntax.impl.BinaryNode;
import org.openl.syntax.impl.ISyntaxConstants;
import org.openl.syntax.impl.IdentifierNode;

class FullClassnameSupport {

    private static StringBuilder tryFixChainWithPackage(ISyntaxNode syntaxNode, IBindingContext bindingContext) {
        if (syntaxNode instanceof IdentifierNode) {
            return new StringBuilder(syntaxNode.getText());
        } else if ("chain.suffix.dot.identifier".equals(syntaxNode.getType())) {
            StringBuilder sb = tryFixChainWithPackage(syntaxNode.getChild(0), bindingContext);
            if (bindingContext.findType(ISyntaxConstants.THIS_NAMESPACE, sb.toString()) != null) {
                try {
                    Field field = BinaryNode.class.getDeclaredField("left");
                    field.setAccessible(true);
                    ISyntaxNode node = syntaxNode.getChild(0);
                    field.set(syntaxNode,
                        new IdentifierNode("identifier", node.getSourceLocation(), sb.toString(), node.getModule()));
                } catch (NoSuchFieldException | IllegalAccessException ignored) {
                }
            }
            sb.append(".");
            sb.append(tryFixChainWithPackage(syntaxNode.getChild(1), bindingContext));
            return sb;
        }
        throw new OpenlNotCheckedException();
    }

    static void rec(ISyntaxNode syntaxNode, IBindingContext bindingContext) {
        if (syntaxNode == null) {
            return;
        }
        if ("chain.suffix.dot.identifier".equals(syntaxNode.getType())) {
            try {
                String fieldName = tryFixChainWithPackage(syntaxNode, bindingContext).toString();
                if (bindingContext.findType(ISyntaxConstants.THIS_NAMESPACE, fieldName) != null) {
                    try {
                        Field field = BinaryNode.class.getDeclaredField("left");
                        field.setAccessible(true);
                        if (!(syntaxNode.getParent() instanceof BinaryNode)) {
                            throw new IllegalStateException();
                        }
                        field.set(syntaxNode.getParent(),
                            new IdentifierNode("identifier",
                                syntaxNode.getSourceLocation(),
                                fieldName,
                                syntaxNode.getModule()));
                    } catch (NoSuchFieldException e) {
                    } catch (IllegalAccessException e) {
                    }
                }
            } catch (OpenlNotCheckedException e) {
                int n = syntaxNode.getNumberOfChildren();
                for (int i = 0; i < n; i++) {
                    rec(syntaxNode.getChild(i), bindingContext);
                }
            }
        } else {
            int n = syntaxNode.getNumberOfChildren();
            for (int i = 0; i < n; i++) {
                rec(syntaxNode.getChild(i), bindingContext);
            }
        }
    }

    static void transformIdentifierBindersWithBindingContextInfo(IBindingContext bindingContext,
            IParsedCode parsedCode) {
        ISyntaxNode topNode = parsedCode.getTopNode();
        if (bindingContext != null) {
            rec(topNode, bindingContext);
        }
    }
}
