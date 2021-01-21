package org.openl.engine;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openl.binding.IBindingContext;
import org.openl.exception.OpenlNotCheckedException;
import org.openl.syntax.ISyntaxNode;
import org.openl.syntax.code.IParsedCode;
import org.openl.syntax.impl.BinaryNode;
import org.openl.syntax.impl.ISyntaxConstants;
import org.openl.syntax.impl.IdentifierNode;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;

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

    private static List<String> getIdentifierChain(ISyntaxNode syntaxNode) {
        if (syntaxNode instanceof IdentifierNode) {
            List<String> ret = new ArrayList<>();
            ret.add(syntaxNode.getText());
            return ret;
        } else if ("chain.suffix.dot.identifier".equals(syntaxNode.getType())) {
            List<String> s = getIdentifierChain(syntaxNode.getChild(0));
            s.addAll(getIdentifierChain(syntaxNode.getChild(1)));
            return s;
        }
        throw new OpenlNotCheckedException();
    }

    static void rec(ISyntaxNode syntaxNode, IBindingContext bindingContext, Map<String, String> localVariables) {
        if (syntaxNode == null) {
            return;
        }
        if ("local.var.declaration".equals(syntaxNode.getType())) {
            if ("identifier".equals(syntaxNode.getChild(1).getType())) {
                localVariables.put(syntaxNode.getChild(1).getText(),
                        syntaxNode.getChild(0).getChild(0).getText());
            } else if ("local.var.name.init".equals(syntaxNode.getChild(1).getType())){
                localVariables.put(syntaxNode.getChild(1).getChild(0).getText(),
                        syntaxNode.getChild(0).getChild(0).getText());
            } else {
                throw new IllegalStateException("Unsupported syntax node type");
            }
        }
        if ("chain.suffix.dot.identifier".equals(syntaxNode.getType())) {
            try {
                List<String> identifierChain = getIdentifierChain(syntaxNode);
                String variableName = identifierChain.get(0);
                String variableType = localVariables.get(variableName);
                IOpenClass type = null;
                if (variableType != null) {
                    type = bindingContext.findType(ISyntaxConstants.THIS_NAMESPACE, variableType);
                } else {
                    IOpenField openField = bindingContext.findVar(ISyntaxConstants.THIS_NAMESPACE, variableName, true);
                    if (openField != null) {
                        type = openField.getType();
                    }
                }
                int i = 1;
                while (type != null && i < identifierChain.size()) {
                    try {
                        IOpenField openField = type.getField(identifierChain.get(i));
                        type = openField != null ? openField.getType() : null;
                    } catch (Exception | LinkageError e) {
                        type = null;
                    }
                    i++;
                }
                if (type == null) {
                    String fullClassName = tryFixChainWithPackage(syntaxNode, bindingContext).toString();
                    if (bindingContext.findType(ISyntaxConstants.THIS_NAMESPACE, fullClassName) != null) {
                        try {
                            Field field = BinaryNode.class.getDeclaredField("left");
                            field.setAccessible(true);
                            if (!(syntaxNode.getParent() instanceof BinaryNode)) {
                                throw new IllegalStateException();
                            }
                            field.set(syntaxNode.getParent(),
                                new IdentifierNode("identifier",
                                    syntaxNode.getSourceLocation(),
                                    fullClassName,
                                    syntaxNode.getModule()));
                        } catch (IllegalAccessException | NoSuchFieldException ignored) {
                        }
                    }
                }
            } catch (OpenlNotCheckedException e) {
                int n = syntaxNode.getNumberOfChildren();
                for (int i = 0; i < n; i++) {
                    rec(syntaxNode.getChild(i), bindingContext, localVariables);
                }
            }
        } else {
            int n = syntaxNode.getNumberOfChildren();
            for (int i = 0; i < n; i++) {
                rec(syntaxNode.getChild(i), bindingContext, localVariables);
            }
        }
    }

    static void transformIdentifierBindersWithBindingContextInfo(IBindingContext bindingContext,
            IParsedCode parsedCode) {
        ISyntaxNode topNode = parsedCode.getTopNode();
        if (bindingContext != null) {
            rec(topNode, bindingContext, new HashMap<>());
        }
    }
}
