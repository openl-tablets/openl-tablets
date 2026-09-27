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

    private FullClassnameSupport() {
        // Utility class
    }

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
            // The exception is a signal only and carries no message.
        }
    }

    static void rec(ISyntaxNode syntaxNode, IBindingContext bindingContext, Map<String, String> localVariables) {
        if (syntaxNode == null) {
            return;
        }
        if ("local.var.declaration".equals(syntaxNode.getType())) {
            addLocalVariable(syntaxNode, localVariables);
        } else if ("chain.suffix.dot.identifier".equals(syntaxNode.getType())) {
            try {
                replaceChainWithFullClassName(syntaxNode, bindingContext, localVariables);
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

    private static void addLocalVariable(ISyntaxNode syntaxNode, Map<String, String> localVariables) {
        if ("identifier".equals(syntaxNode.getChild(1).getType())) {
            localVariables.put(syntaxNode.getChild(1).getText(), syntaxNode.getChild(0).getChild(0).getText());
        } else if ("local.var.name.init".equals(syntaxNode.getChild(1).getType())) {
            localVariables.put(syntaxNode.getChild(1).getChild(0).getText(),
                    syntaxNode.getChild(0).getChild(0).getText());
        } else {
            throw new IllegalStateException("Unsupported syntax node type");
        }
    }

    /**
     * Replaces the longest leading part of the identifier chain that names a type with one identifier holding the full
     * class name. Only a part longer than what a local variable or a field of the first name resolves is replaced.
     */
    private static void replaceChainWithFullClassName(ISyntaxNode syntaxNode,
                                                      IBindingContext bindingContext,
                                                      Map<String, String> localVariables)
            throws IdentifierChainException {
        var identifierChain = getIdentifierChain(syntaxNode);
        var variableName = identifierChain.getFirst().getText();
        var variableType = localVariables.get(variableName);
        var varTypeLength = 0;
        if (variableType != null) {
            varTypeLength = calcTypeLength(identifierChain, bindingContext, variableType);
            if (varTypeLength == identifierChain.size()) {
                return;
            }
        }
        var varNameLength = calcVarLength(identifierChain, bindingContext, variableName);
        if (varNameLength == identifierChain.size()) {
            return;
        }
        var fullClassName = new StringBuilder();
        String[] fullClassNames = new String[identifierChain.size()];
        for (var j = 0; j < identifierChain.size(); j++) {
            var syntaxNode1 = identifierChain.get(j);
            if (!fullClassName.isEmpty()) {
                fullClassName.append(".");
            }
            fullClassName.append(syntaxNode1.getText());
            fullClassNames[j] = fullClassName.toString();
        }
        var j = identifierChain.size() - 1;
        while (j >= 0 && j + 1 > varTypeLength && j + 1 > varNameLength) {
            var type = bindingContext.findType(fullClassNames[j]);
            if (type != null) {
                updateSyntaxNode(syntaxNode, identifierChain, getOriginalFullClassName(identifierChain, j), j);
                break;
            }
            j--;
        }
    }

    /**
     * Joins the original texts of the chain elements up to the given index with dots.
     */
    private static String getOriginalFullClassName(List<ISyntaxNode> identifierChain, int j) {
        var originalFullClassName = new StringBuilder();
        for (var k = 0; k < j + 1; k++) {
            var syntaxNode1 = identifierChain.get(k);
            if (!originalFullClassName.isEmpty()) {
                originalFullClassName.append(".");
            }
            originalFullClassName.append(
                    syntaxNode1 instanceof IdentifierNode in ? in.getOriginalText()
                            : syntaxNode1.getText());
        }
        return originalFullClassName.toString();
    }

    /**
     * Counts the leading chain elements that resolve through a local variable of the given type. An ambiguous type
     * resolves none.
     */
    private static int calcTypeLength(List<ISyntaxNode> identifierChain,
                                      IBindingContext bindingContext,
                                      String variableType) {
        try {
            var type = bindingContext.findType(variableType);
            return calcLength(identifierChain, type);
        } catch (AmbiguousTypeException e) {
            return 0;
        }
    }

    /**
     * Counts the leading chain elements that resolve through the variable of the given name. An ambiguous variable
     * resolves none.
     */
    private static int calcVarLength(List<ISyntaxNode> identifierChain,
                                     IBindingContext bindingContext,
                                     String variableName) {
        try {
            var field = bindingContext.findVar(ISyntaxConstants.THIS_NAMESPACE, variableName, true);
            return calcLength(identifierChain, field != null ? field.getType() : null);
        } catch (AmbiguousFieldException e) {
            return 0;
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
                if (f == null) {
                    if (j == identifierChain.size() - 1
                            && type.getMethods().stream().anyMatch(e -> e.getName().equals(part))) {
                        return identifierChain.size();
                    }
                    break;
                }
                type = f.getType();
                ret++;
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
