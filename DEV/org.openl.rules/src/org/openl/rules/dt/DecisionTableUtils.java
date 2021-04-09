package org.openl.rules.dt;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.mutable.MutableBoolean;
import org.openl.binding.IBoundMethodNode;
import org.openl.rules.dt.element.ICondition;
import org.openl.rules.lang.xls.binding.DTColumnsDefinition;
import org.openl.syntax.ISyntaxNode;
import org.openl.syntax.impl.IdentifierNode;
import org.openl.types.impl.CompositeMethod;

public class DecisionTableUtils {

    private DecisionTableUtils() {
    }

    public static List<IdentifierNode> retrieveIdentifierNodes(ICondition dtCondition) {
        return retrieveIdentifierNodes(((CompositeMethod) dtCondition.getMethod()));
    }

    static List<IdentifierNode> retrieveIdentifierNodes(DTColumnsDefinition definition) {
        return retrieveIdentifierNodes(definition.getCompositeMethod());
    }

    public static List<IdentifierNode> retrieveIdentifierNodes(CompositeMethod dtCompositeMethod) {
        List<IdentifierNode> identifierNodes = new ArrayList<>();
        IBoundMethodNode methodNode = dtCompositeMethod.getMethodBodyBoundNode();
        if (methodNode != null) {
            parseAndCollectIdentifierNodes(methodNode.getSyntaxNode(),
                new MutableBoolean(false),
                false,
                identifierNodes);
        }
        return identifierNodes;
    }

    private static void parseAndCollectIdentifierNodes(ISyntaxNode node,
            MutableBoolean chain,
            boolean inChain,
            List<IdentifierNode> identifierNodes) {
        for (int i = 0; i < node.getNumberOfChildren(); i++) {
            if ("identifier".equals(node.getChild(i).getType())) {
                if (!chain.booleanValue()) {
                    identifierNodes.add((IdentifierNode) node.getChild(i));
                    if (inChain) {
                        chain.setTrue();
                    }
                }
            } else if ("chain".equals(node.getChild(i).getType())) {
                boolean f = chain.booleanValue();
                parseAndCollectIdentifierNodes(node.getChild(i), chain, true, identifierNodes);
                chain.setValue(f);
            } else if ("function".equals(node.getChild(i).getType())) {
                parseAndCollectIdentifierNodes(node.getChild(i), new MutableBoolean(false), false, identifierNodes);
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
