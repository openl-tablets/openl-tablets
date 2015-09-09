package org.openl.engine;

import java.lang.reflect.Field;

import org.openl.binding.IBindingContextDelegator;
import org.openl.exception.OpenlNotCheckedException;
import org.openl.syntax.ISyntaxNode;
import org.openl.syntax.code.IParsedCode;
import org.openl.syntax.impl.BinaryNode;
import org.openl.syntax.impl.ISyntaxConstants;
import org.openl.syntax.impl.IdentifierNode;

class FullClassnameSupport {

    private static StringBuilder tryFixChainWithPackage(ISyntaxNode syntaxNode,
            IBindingContextDelegator bindingContextDelegator) {
        if (syntaxNode instanceof IdentifierNode) {
            IdentifierNode identifierNode = (IdentifierNode) syntaxNode;
            return new StringBuilder(identifierNode.getIdentifier());
        } else {
            if (syntaxNode instanceof BinaryNode) {
                BinaryNode binaryNode = (BinaryNode) syntaxNode;
                if ("chain.suffix.dot.identifier".equals(binaryNode.getType())) {
                    StringBuilder sb = tryFixChainWithPackage(binaryNode.getChild(0), bindingContextDelegator);
                    if (bindingContextDelegator.findType(ISyntaxConstants.THIS_NAMESPACE, sb.toString()) != null) {
                        try {
                            Field field = BinaryNode.class.getDeclaredField("left");
                            field.setAccessible(true);
                            ISyntaxNode node = binaryNode.getChild(0);
                            field.set(binaryNode,
                                new IdentifierNode("identifier",
                                    node.getSourceLocation(),
                                    sb.toString(),
                                    node.getModule()));
                        } catch (NoSuchFieldException e) {
                        } catch (IllegalAccessException e) {
                        }
                    }
                    sb.append(".");
                    sb.append(tryFixChainWithPackage(binaryNode.getChild(1), bindingContextDelegator));
                    return sb;
                } else {
                    throw new OpenlNotCheckedException();
                }
            }
        }
        throw new OpenlNotCheckedException();
    }

    static void rec(ISyntaxNode syntaxNode, IBindingContextDelegator bindingContextDelegator) {
        if (syntaxNode == null) {
            return;
        }
        if ("chain.suffix.dot.identifier".equals(syntaxNode.getType())) {
            try { 
                String fieldName = tryFixChainWithPackage(syntaxNode, bindingContextDelegator).toString();
                if (bindingContextDelegator.findType(ISyntaxConstants.THIS_NAMESPACE, fieldName) != null) {
                    try {
                        Field field = BinaryNode.class.getDeclaredField("left");
                        field.setAccessible(true);
                        if (!(syntaxNode.getParent() instanceof BinaryNode)){
                            throw new IllegalStateException();
                        }
                        field.set(syntaxNode.getParent(), new IdentifierNode("identifier",
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
                    rec(syntaxNode.getChild(i), bindingContextDelegator);
                }
            }
        } else {
            int n = syntaxNode.getNumberOfChildren();
            for (int i = 0; i < n; i++) {
                rec(syntaxNode.getChild(i), bindingContextDelegator);
            }
        }
    }

    static void transformIdentifierBindersWithBindingContextInfo(IBindingContextDelegator bindingContextDelegator,
            IParsedCode parsedCode) {
        ISyntaxNode topNode = parsedCode.getTopNode();
        if (bindingContextDelegator != null) {
            rec(topNode, bindingContextDelegator);
        }
    }
}
