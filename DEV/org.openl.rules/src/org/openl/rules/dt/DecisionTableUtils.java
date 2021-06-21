package org.openl.rules.dt;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.mutable.MutableBoolean;
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
        parseAndCollectIdentifierNodes(dtCompositeMethod.getMethodBodyBoundNode().getSyntaxNode(),
            new MutableBoolean(false),
            false,
            identifierNodes);
        return identifierNodes;
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
        return ((CompositeMethod) dtCondition.getMethod()).getMethodBodyBoundNode()
            .getSyntaxNode()
            .getModule()
            .getCode();
    }

}
