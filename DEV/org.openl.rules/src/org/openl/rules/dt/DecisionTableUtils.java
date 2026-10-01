package org.openl.rules.dt;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.mutable.MutableBoolean;

import org.openl.binding.impl.IdentifierSequenceBinder;
import org.openl.rules.dt.element.ICondition;
import org.openl.rules.lang.xls.binding.ExpressionIdentifier;
import org.openl.syntax.ISyntaxNode;
import org.openl.syntax.impl.IdentifierNode;
import org.openl.types.impl.CompositeMethod;

public class DecisionTableUtils {

    private DecisionTableUtils() {
    }

    public static List<ExpressionIdentifier> extractIdentifiers(ICondition dtCondition) {
        return extractIdentifiers(((CompositeMethod) dtCondition.getMethod()).getMethodBodyBoundNode().getSyntaxNode());
    }

    public static List<ExpressionIdentifier> extractIdentifiers(ISyntaxNode syntaxNode) {
        var identifierNodes = new ArrayList<IdentifierNode>();
        if (syntaxNode != null) {
            parseAndCollectIdentifierNodes(syntaxNode, new MutableBoolean(false), false, identifierNodes);
        }
        return identifierNodes.stream()
                .map(e -> new ExpressionIdentifier(e.getIdentifier(), e.getLocation()))
                .toList();
    }

    private static void parseAndCollectIdentifierNodes(ISyntaxNode node,
                                                       MutableBoolean chain,
                                                       boolean inChain,
                                                       List<IdentifierNode> identifierNodes) {
        for (var i = 0; i < node.getNumberOfChildren(); i++) {
            final var child = node.getChild(i);
            final var childType = child.getType();
            if ("identifier".equals(childType) || "identifier.sequence".equals(childType)) {
                collectIdentifierNode(child, childType, chain, inChain, identifierNodes);
            } else if ("chain".equals(childType)) {
                var f = chain.booleanValue();
                parseAndCollectIdentifierNodes(child, chain, true, identifierNodes);
                chain.setValue(f);
            } else if ("function".equals(childType)) {
                parseAndCollectIdentifierNodes(child, new MutableBoolean(false), false, identifierNodes);
            } else if ("selectfirst.index".equals(childType) || "selectall.index".equals(childType) || "transform.index"
                    .equals(childType) || "transformunique.index".equals(childType)) {
                parseAndCollectIdentifierNodes(child, new MutableBoolean(false), false, identifierNodes);
            } else {
                parseAndCollectIdentifierNodes(node.getChild(i), chain, inChain, identifierNodes);
            }
        }
    }

    private static void collectIdentifierNode(ISyntaxNode child,
                                              String childType,
                                              MutableBoolean chain,
                                              boolean inChain,
                                              List<IdentifierNode> identifierNodes) {
        IdentifierNode identifierNode;
        if ("identifier.sequence".equals(childType)) {
            identifierNode = IdentifierSequenceBinder.toIdentifierNode(child);
        } else {
            identifierNode = (IdentifierNode) child;
        }
        if (!chain.booleanValue()) {
            identifierNodes.add(identifierNode);
            if (inChain) {
                chain.setTrue();
            }
        }
    }

    public static String getConditionSourceCode(ICondition dtCondition) {
        var methodNode = ((CompositeMethod) dtCondition.getMethod()).getMethodBodyBoundNode();
        return methodNode == null ? "" : methodNode.getSyntaxNode().getModule().getCode();
    }

}
