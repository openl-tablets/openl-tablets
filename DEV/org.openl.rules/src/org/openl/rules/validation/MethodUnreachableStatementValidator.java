package org.openl.rules.validation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.openl.binding.IBoundNode;
import org.openl.binding.impl.BlockNode;
import org.openl.binding.impl.CastNode;
import org.openl.binding.impl.IfNode;
import org.openl.binding.impl.LoopNode;
import org.openl.message.OpenLMessage;
import org.openl.message.OpenLMessagesUtils;
import org.openl.rules.lang.xls.binding.wrapper.WrapperLogic;
import org.openl.rules.method.table.TableMethod;
import org.openl.rules.types.OpenMethodDispatcher;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;
import org.openl.validation.IOpenLValidator;
import org.openl.validation.ValidationResult;

/**
 * @author Vladyslav Pikus
 */
public class MethodUnreachableStatementValidator implements IOpenLValidator {

    private static class Holder {
        private static final MethodUnreachableStatementValidator INSTANCE = new MethodUnreachableStatementValidator();
    }

    public static MethodUnreachableStatementValidator getInstance() {
        return Holder.INSTANCE;
    }

    private MethodUnreachableStatementValidator() {
    }

    @Override
    public ValidationResult validate(IOpenClass openClass) {
        List<OpenLMessage> messages = new ArrayList<>();
        visit(openClass.getMethods(), messages);
        return ValidationUtils.withMessages(messages);
    }

    private void visit(Collection<IOpenMethod> methods, List<OpenLMessage> messages) {
        for (IOpenMethod method : methods) {
            if (method instanceof OpenMethodDispatcher) {
                visit(((OpenMethodDispatcher) method).getCandidates(), messages);
            } else {
                visit(WrapperLogic.unwrapOpenMethod(method), messages);
            }
        }
    }

    private void visit(IOpenMethod method, List<OpenLMessage> messages) {
        if (!(method instanceof TableMethod)) {
            return;
        }
        IBoundNode boundNode = ((TableMethod) method).getCompositeMethod().getMethodBodyBoundNode();
        visitStatement(boundNode, messages);
    }

    private void visitStatement(IBoundNode blockNode, List<OpenLMessage> messages) {
        if (blockNode == null) {
            return;
        }
        if (blockNode instanceof LoopNode) {
            visitStatement(((LoopNode) blockNode).getBlockCodeNode(), messages);
        } else if (blockNode instanceof IfNode) {
            visitStatement(((IfNode) blockNode).getThenNode(), messages);
            visitStatement(((IfNode) blockNode).getElseNode(), messages);
        } else if (blockNode instanceof CastNode) {
            visitStatement(blockNode.getChildren()[0], messages);
        } else if (blockNode instanceof BlockNode) {
            final IBoundNode[] children = blockNode.getChildren();
            if (children.length == 0) {
                return;
            }
            for (int i = 0; i < children.length - 1; i++) {
                checkPair(children[i], children[i + 1], messages);
                visitStatement(children[i], messages);
            }
            visitStatement(children[children.length - 1], messages);
        }
    }

    private void checkPair(IBoundNode prevNode, IBoundNode node, List<OpenLMessage> messages) {
        if (!BoundNodeAnalyzingUtils.nodeMayCompleteNormally(prevNode)) {
            messages.add(OpenLMessagesUtils.newWarnMessage("Unreachable statement", node.getSyntaxNode()));
        }
    }

}
