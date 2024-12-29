package org.openl.binding.impl;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.openl.binding.IBindingContext;
import org.openl.binding.IBoundNode;
import org.openl.binding.MethodUtil;
import org.openl.binding.impl.module.ModuleSpecificOpenMethod;
import org.openl.message.OpenLMessage;
import org.openl.syntax.ISyntaxNode;
import org.openl.syntax.exception.SyntaxNodeException;
import org.openl.syntax.impl.IdentifierNode;
import org.openl.util.MessageUtils;

/**
 * @author snshor
 */
public class NewNodeBinder extends ANodeBinder {

    /*
     * (non-Javadoc)
     *
     * @see org.openl.binding.INodeBinder#bind(org.openl.parser.ISyntaxNode, org.openl.env.IOpenEnv,
     * org.openl.binding.IBindingContext)
     */
    @Override
    public IBoundNode bind(ISyntaxNode node, IBindingContext bindingContext) throws Exception {

        int childrenCount = node.getNumberOfChildren();

        if (childrenCount < 1) {
            return makeErrorNode("New node must have at least one sub-node.", node, bindingContext);
        }

        var typeNode = node.getChild(0);
        var typeName = ((IdentifierNode) typeNode).getIdentifier();
        var type = bindingContext.findType(typeName);

        if (type == null) {
            return makeErrorNode(MessageUtils.getTypeNotFoundMessage(typeName), typeNode, bindingContext);
        }
        if (type.getInstanceClass() == null) {
            return makeErrorNode(MessageUtils.getTypeDefinedErrorMessage(typeName), typeNode, bindingContext);
        }
        bindingContext.pushErrors();
        bindingContext.pushMessages();
        boolean errorsAndMessagesPopped = false;
        boolean sugarConstructor = false;
        List<SyntaxNodeException> syntaxNodeExceptions = Collections.emptyList();
        Collection<OpenLMessage> openLMessages = Collections.emptyList();
        try {
            var children = bindChildren(node, bindingContext, 1, childrenCount);
            syntaxNodeExceptions = bindingContext.popErrors();
            openLMessages = bindingContext.popMessages();
            errorsAndMessagesPopped = true;
            var childNodes = new ISyntaxNode[node.getNumberOfChildren() - 1];
            for (int i = 0; i < childNodes.length; i++) {
                childNodes[i] = node.getChild(i + 1);
            }
            if (hasErrorBoundNode(children)) {
                var iBoundNode = Optional.of(type)
                        .map(t -> ConstructorSugarSupport
                                .makeSugarConstructor(node, childNodes, bindingContext, t, node.getChild(childrenCount - 1)));
                if (iBoundNode.isPresent()) {
                    sugarConstructor = true;
                    return iBoundNode.get();
                }
                return new ErrorBoundNode(node);
            }
            var paramTypes = getTypes(children);
            var methodCaller = ModuleSpecificOpenMethod.findConstructorCaller(type, paramTypes, bindingContext);
            BindHelper.checkOnDeprecation(node, bindingContext, methodCaller);
            if (methodCaller == null) {
                var iBoundNode = Optional.of(type)
                        .map(t -> ConstructorSugarSupport
                                .makeSugarConstructor(node, childNodes, bindingContext, t, node.getChild(childrenCount - 1)));
                if (iBoundNode.isPresent()) {
                    sugarConstructor = true;
                    return iBoundNode.get();
                }
                var constructor = MethodUtil.printMethod(type.getName(), paramTypes);
                return makeErrorNode(MessageUtils.getConstructorNotFoundMessage(constructor), typeNode, bindingContext);
            }
            return new ConstructorParamsNode(new MethodBoundNode(typeNode, methodCaller, children));
        } finally {
            if (!sugarConstructor) {
                bindingContext.addMessages(openLMessages);
                syntaxNodeExceptions.forEach(bindingContext::addError);
            }
            if (!errorsAndMessagesPopped) {
                bindingContext.popErrors();
                bindingContext.popMessages();
            }
        }
    }
}
