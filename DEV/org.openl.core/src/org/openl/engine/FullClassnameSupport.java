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
    static final Field binaryNodeLeftField;

    static {
        Field binaryNodeLeftFieldTmp = null;
        try {
            binaryNodeLeftFieldTmp = BinaryNode.class.getDeclaredField("left");
            binaryNodeLeftFieldTmp.setAccessible(true);
        } catch (NoSuchFieldException ignored) {
        }
        binaryNodeLeftField = binaryNodeLeftFieldTmp;
    }

    private static List<ISyntaxNode> getIdentifierChain(ISyntaxNode syntaxNode) {
        if (syntaxNode instanceof IdentifierNode) {
            List<ISyntaxNode> ret = new ArrayList<>();
            ret.add(syntaxNode);
            return ret;
        } else if ("chain.suffix.dot.identifier".equals(syntaxNode.getType())) {
            List<ISyntaxNode> s = getIdentifierChain(syntaxNode.getChild(0));
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
                localVariables.put(syntaxNode.getChild(1).getText(), syntaxNode.getChild(0).getChild(0).getText());
            } else if ("local.var.name.init".equals(syntaxNode.getChild(1).getType())) {
                localVariables.put(syntaxNode.getChild(1).getChild(0).getText(),
                    syntaxNode.getChild(0).getChild(0).getText());
            } else {
                throw new IllegalStateException("Unsupported syntax node type");
            }
        } else if ("chain.suffix.dot.identifier".equals(syntaxNode.getType())) {
            try {
                List<ISyntaxNode> identifierChain = getIdentifierChain(syntaxNode);
                String variableName = identifierChain.get(0).getText();
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
                if (type == null) {
                    StringBuilder fullClassName = new StringBuilder();
                    String[] fullClassNames = new String[identifierChain.size()];
                    boolean f = false;
                    for (int j = 0; j < identifierChain.size(); j++) {
                        ISyntaxNode syntaxNode1 = identifierChain.get(j);
                        if (f) {
                            fullClassName.append(".");
                        } else {
                            f = true;
                        }
                        fullClassName.append(syntaxNode1.getText());
                        fullClassNames[j] = fullClassName.toString();
                    }

                    int j = identifierChain.size() - 1;
                    while (j >= 0) {
                        type = bindingContext.findType(ISyntaxConstants.THIS_NAMESPACE, fullClassNames[j]);
                        if (type != null) {
                            updateSyntaxNode(syntaxNode, identifierChain, fullClassNames[j], j);
                            break;
                        }
                        j--;
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

    private static void updateSyntaxNode(ISyntaxNode syntaxNode,
            List<ISyntaxNode> identifierChain,
            String fullClassName,
            int j) {
        try {
            ISyntaxNode nodeToChange;
            if (j < identifierChain.size() - 1) {
                nodeToChange = identifierChain.get(j + 1).getParent();
            } else {
                nodeToChange = syntaxNode.getParent();
            }
            if (!(nodeToChange instanceof BinaryNode)) {
                throw new IllegalStateException();
            }
            binaryNodeLeftField.set(nodeToChange,
                new IdentifierNode("identifier",
                    nodeToChange.getChild(0).getSourceLocation(),
                    fullClassName,
                    nodeToChange.getChild(0).getModule()));
        } catch (IllegalAccessException ignored) {
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
