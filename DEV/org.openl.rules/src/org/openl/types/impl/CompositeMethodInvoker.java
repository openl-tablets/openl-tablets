package org.openl.types.impl;


import lombok.extern.slf4j.Slf4j;

import org.openl.IOpenRunner;
import org.openl.binding.IBoundMethodNode;
import org.openl.binding.IBoundNode;
import org.openl.binding.impl.ABoundNode;
import org.openl.binding.impl.BlockNode;
import org.openl.binding.impl.ControlSignalReturn;
import org.openl.types.Invokable;
import org.openl.vm.IRuntimeEnv;

/**
 * Invoker for {@link CompositeMethod}.
 *
 * @author DLiauchuk
 */
@Slf4j
public class CompositeMethodInvoker implements Invokable {

    private IBoundMethodNode methodBodyBoundNode;

    private IBoundNode expressionNode;

    public CompositeMethodInvoker(IBoundMethodNode methodBodyBoundNode, CompositeMethod method) {
        this.methodBodyBoundNode = methodBodyBoundNode;

        optimizeMethodCall(methodBodyBoundNode, method);
    }

    private void optimizeMethodCall(IBoundMethodNode methodBodyBoundNode, CompositeMethod method) {

        if (methodBodyBoundNode instanceof BlockNode mbb) {
            IBoundNode[] children = mbb.getChildren();
            if (children != null && children.length == 1 && mbb.getLocalFrameSize() == method.getSignature()
                    .getNumberOfParameters()) {
                expressionNode = children[0];
            }
        }
        if (expressionNode != null) {
            this.methodBodyBoundNode = null;
        }

    }

    public void removeDebugInformation() {
        if (expressionNode != null) {
            clearSyntaxNodes(expressionNode);
        }
        if (methodBodyBoundNode != null) {
            clearSyntaxNodes(methodBodyBoundNode);
        }
    }

    private static void clearSyntaxNodes(IBoundNode boundNode) {
        if (boundNode instanceof ABoundNode node) {
            node.setSyntaxNode(null);
        }
        IBoundNode[] children = boundNode.getChildren();
        if (children != null) {
            for (IBoundNode child : children) {
                if (child != null) {
                    clearSyntaxNodes(child);
                }
            }
        }
    }

    @Override
    public Object invoke(Object target, Object[] params, IRuntimeEnv env) {
        try {
            env.pushThis(target);
            IOpenRunner runner = env.getRunner();

            return expressionNode == null ? runner.run(methodBodyBoundNode, params, env)
                    : runner.runExpression(expressionNode, params, env);
        } catch (ControlSignalReturn csret) {
            log.debug("Error occurred: ", csret);
            return csret.getReturnValue();
        } finally {
            env.popThis();
        }
    }

}
