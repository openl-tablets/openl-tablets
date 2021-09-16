package org.openl.rules.dt;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.mutable.MutableBoolean;
import org.openl.binding.IBoundMethodNode;
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
        List<IdentifierNode> identifierNodes = new ArrayList<>();
        if (syntaxNode != null) {
            parseAndCollectIdentifierNodes(syntaxNode, new MutableBoolean(false), false, identifierNodes);
        }
        return identifierNodes.stream()
            .map(e -> new ExpressionIdentifier(e.getIdentifier(), e.getLocation()))
            .collect(Collectors.toList());
    }

    private static void parseAndCollectIdentifierNodes(ISyntaxNode node,
            MutableBoolean chain,
            boolean inChain,
            List<IdentifierNode> identifierNodes) {
        for (int i = 0; i < node.getNumberOfChildren(); i++) {
            final ISyntaxNode child = node.getChild(i);
            final String childType = child.getType();
            if ("identifier".equals(childType)) {
                if (!chain.booleanValue()) {
                    identifierNodes.add((IdentifierNode) child);
                    if (inChain) {
                        chain.setTrue();
                    }
                }
            } else if ("chain".equals(childType)) {
                boolean f = chain.booleanValue();
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

    public static String getConditionSourceCode(ICondition dtCondition) {
        IBoundMethodNode methodNode = ((CompositeMethod) dtCondition.getMethod()).getMethodBodyBoundNode();
        return methodNode == null ? "" : methodNode.getSyntaxNode().getModule().getCode();
    }

}
