package org.openl.engine;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openl.binding.IBindingContext;
import org.openl.binding.exception.AmbiguousFieldException;
import org.openl.binding.exception.AmbiguousTypeException;
import org.openl.syntax.ISyntaxNode;
import org.openl.syntax.code.IParsedCode;
import org.openl.syntax.impl.BinaryNode;
import org.openl.syntax.impl.ISyntaxConstants;
import org.openl.syntax.impl.IdentifierNode;
import org.openl.syntax.impl.UnaryNode;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;

class FullClassnameSupport {

    private static List<ISyntaxNode> getIdentifierChain(ISyntaxNode syntaxNode) throws IdentifierChainException {
        if (syntaxNode instanceof IdentifierNode) {
            var ret = new ArrayList<ISyntaxNode>();
            ret.add(syntaxNode);
            return ret;
        } else if ("chain.suffix.dot.identifier".equals(syntaxNode.getType())) {
            var s = getIdentifierChain(syntaxNode.getChild(0));
            s.addAll(getIdentifierChain(syntaxNode.getChild(1)));
            return s;
        }
        throw new IdentifierChainException();
    }

    private static class IdentifierChainException extends Exception {
        public IdentifierChainException() {
        }
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
                var identifierChain = getIdentifierChain(syntaxNode);
                var variableName = identifierChain.getFirst().getText();
                var variableType = localVariables.get(variableName);
                var varTypeLength = 0;
                if (variableType != null) {
                    try {
                        var type = bindingContext.findType(variableType);
                        varTypeLength = calcLength(identifierChain, type);
                        if (varTypeLength == identifierChain.size()) {
                            return;
                        }
                    } catch (AmbiguousTypeException e) {
                        varTypeLength = 0;
                    }
                }
                int varNameLength;
                try {
                    var var = bindingContext.findVar(ISyntaxConstants.THIS_NAMESPACE, variableName, true);
                    varNameLength = calcLength(identifierChain, var != null ? var.getType() : null);
                    if (varNameLength == identifierChain.size()) {
                        return;
                    }
                } catch (AmbiguousFieldException e) {
                    varNameLength = 0;
                }
                var fullClassName = new StringBuilder();
                String[] fullClassNames = new String[identifierChain.size()];
                for (var j = 0; j < identifierChain.size(); j++) {
                    var syntaxNode1 = identifierChain.get(j);
                    if (fullClassName.length() > 0) {
                        fullClassName.append(".");
                    }
                    fullClassName.append(syntaxNode1.getText());
                    fullClassNames[j] = fullClassName.toString();
                }
                var j = identifierChain.size() - 1;
                while (j >= 0 && j + 1 > varTypeLength && j + 1 > varNameLength) {
                    var type = bindingContext.findType(fullClassNames[j]);
                    if (type != null) {
                        var originalFullClassName = new StringBuilder();
                        for (var k = 0; k < j + 1; k++) {
                            var syntaxNode1 = identifierChain.get(k);
                            if (originalFullClassName.length() > 0) {
                                originalFullClassName.append(".");
                            }
                            originalFullClassName.append(
                                    syntaxNode1 instanceof IdentifierNode in ? in.getOriginalText()
                                            : syntaxNode1.getText());
                        }
                        updateSyntaxNode(syntaxNode, identifierChain, originalFullClassName.toString(), j);
                        break;
                    }
                    j--;
                }
            } catch (IdentifierChainException e) {
                var n = syntaxNode.getNumberOfChildren();
                for (var i = 0; i < n; i++) {
                    rec(syntaxNode.getChild(i), bindingContext, localVariables);
                }
            }
        } else {
            var n = syntaxNode.getNumberOfChildren();
            for (var i = 0; i < n; i++) {
                rec(syntaxNode.getChild(i), bindingContext, localVariables);
            }
        }
    }

    private static Integer calcLength(List<ISyntaxNode> identifierChain, IOpenClass type) {
        var ret = 0;
        if (type != null) {
            ret++;
            for (var j = 1; j < identifierChain.size(); j++) {
                var part = identifierChain.get(j).getText();
                IOpenField f;
                try {
                    f = type.getField(part);
                } catch (Exception | LinkageError e) {
                    return ret;
                }
                if (f != null) {
                    type = f.getType();
                    ret++;
                    continue;
                } else if (j == identifierChain.size() - 1) {
                    if (type.getMethods().stream().anyMatch(e -> e.getName().equals(part))) {
                        return identifierChain.size();
                    }
                }
                break;
            }
        }
        return ret;
    }

    private static void updateSyntaxNode(ISyntaxNode syntaxNode,
                                         List<ISyntaxNode> identifierChain,
                                         String fullClassName,
                                         int j) {
        ISyntaxNode nodeToChange;
        if (j < identifierChain.size() - 1) {
            nodeToChange = identifierChain.get(j + 1).getParent();
        } else {
            nodeToChange = syntaxNode.getParent();
        }
        var newIdentifierNode = new IdentifierNode("identifier",
                nodeToChange.getChild(0).getSourceLocation(),
                fullClassName,
                nodeToChange.getChild(0).getModule());
        switch (nodeToChange) {
            case BinaryNode node1 -> node1.left = newIdentifierNode;
            case UnaryNode node -> node.left = newIdentifierNode;
            case null, default -> throw new IllegalStateException();
        }
    }

    static void transformIdentifierBindersWithBindingContextInfo(IBindingContext bindingContext,
                                                                 IParsedCode parsedCode) {
        var topNode = parsedCode.getTopNode();
        if (bindingContext != null) {
            rec(topNode, bindingContext, new HashMap<>());
        }
    }
}
