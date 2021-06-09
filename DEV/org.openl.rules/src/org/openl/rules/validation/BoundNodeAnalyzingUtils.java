package org.openl.rules.validation;

import org.openl.binding.IBoundNode;
import org.openl.binding.impl.BlockNode;
import org.openl.binding.impl.CastNode;
import org.openl.binding.impl.IfNode;
import org.openl.binding.impl.LiteralBoundNode;
import org.openl.binding.impl.LoopNode;
import org.openl.binding.impl.ReturnNode;

/**
 * @author Vladyslav Pikus
 */
final class BoundNodeAnalyzingUtils {

    private BoundNodeAnalyzingUtils() {
    }

    public static boolean nodeMayCompleteNormally(IBoundNode node) {
        if (node == null) {
            return true;
        }
        if (node instanceof ReturnNode) {
            return false;
        } else if (node instanceof LoopNode) {
            return loopNodeMayCompleteNormally((LoopNode) node);
        } else if (node instanceof IfNode) {
            return ifNodeMayCompleteNormally((IfNode) node);
        } else if (node instanceof CastNode) {
            return nodeMayCompleteNormally(node.getChildren()[0]);
        } else if (node instanceof BlockNode) {
            return blockNodeMayCompleteNormally((BlockNode) node);
        }
        return true;
    }

    private static boolean loopNodeMayCompleteNormally(LoopNode loopNode) {
        IBoundNode conditionNode = loopNode.getConditionNode();
        if (conditionNode == null) {
            return false;
        }
        final Object value = computeConstantExpression(conditionNode);
        return Boolean.TRUE != value;
    }

    private static boolean ifNodeMayCompleteNormally(IfNode ifNode) {
        final IBoundNode conditionNode = ifNode.getConditionNode();
        if (conditionNode instanceof LiteralBoundNode) {
            final Object value = computeConstantExpression(conditionNode);
            if (Boolean.TRUE == value) {
                return nodeMayCompleteNormally(ifNode.getThenNode());
            }
            if (Boolean.FALSE == value) {
                return nodeMayCompleteNormally(ifNode.getElseNode());
            }
        }
        return nodeMayCompleteNormally(ifNode.getThenNode()) || nodeMayCompleteNormally(ifNode.getElseNode());
    }

    private static boolean blockNodeMayCompleteNormally(BlockNode blockNode) {
        if (blockNode == null) {
            return true;
        }
        IBoundNode[] children = blockNode.getChildren();
        for (IBoundNode child : children) {
            if (!nodeMayCompleteNormally(child)) {
                return false;
            }
        }
        return true;
    }

    private static Object computeConstantExpression(IBoundNode conditionNode) {
        if (!(conditionNode instanceof LiteralBoundNode)) {
            return null;
        }
        LiteralBoundNode literalNode = (LiteralBoundNode) conditionNode;
        return literalNode.getValue();
    }

}
