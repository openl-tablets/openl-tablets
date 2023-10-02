package org.openl.binding.impl;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

import org.openl.base.INamedThing;
import org.openl.binding.IBindingContext;
import org.openl.binding.IBoundNode;
import org.openl.binding.impl.method.MethodSearch;
import org.openl.syntax.ISyntaxNode;
import org.openl.syntax.impl.ISyntaxConstants;
import org.openl.syntax.impl.IdentifierNode;
import org.openl.types.IOpenClass;
import org.openl.util.MessageUtils;

public class ConstructorSugarSupport {

    public static IBoundNode makeSugarConstructor(ISyntaxNode node,
            ISyntaxNode[] childNodes,
            IBindingContext bindingContext,
            IOpenClass type,
            ISyntaxNode typeNode) {
        bindingContext.pushMessages();
        bindingContext.pushErrors();
        try {
            if (type.getInstanceClass() == null) {
                return ANodeBinder.makeErrorNode(MessageUtils
                    .getTypeDefinedErrorMessage(((IdentifierNode) typeNode).getIdentifier()), typeNode, bindingContext);
            }
            var params = new ArrayList<IBoundNode>();
            var namedParams = new HashMap<String, ISyntaxNode>();
            var isAllParamsAssign = true;
            ISyntaxNode duplicatedParamSyntaxNode = null;
            var isAllParamsNoAssign = true;
            try {
                bindingContext.pushLocalVarContext();
                var localVar = bindingContext
                    .addVar(ISyntaxConstants.THIS_NAMESPACE, bindingContext.getTemporaryVarName(), type);
                var varBindingContext = TypeBindingContext.create(bindingContext, localVar, 1);
                for (var child : childNodes) {
                    String childType = child.getType();
                    if ("op.assign".equals(childType)) {
                        isAllParamsNoAssign = false;
                        var iBoundNode = ANodeBinder.bindChildNode(child, varBindingContext);
                        var paramNameSyntaxNode = child.getChild(0);
                        var paramName = paramNameSyntaxNode.getText();
                        if (namedParams.containsKey(paramName)) {
                            duplicatedParamSyntaxNode = paramNameSyntaxNode;
                        }
                        namedParams.put(paramName, paramNameSyntaxNode);
                        params.add(iBoundNode);
                    } else {
                        isAllParamsAssign = false;
                        var iBoundNode = ANodeBinder.bindChildNode(child, bindingContext);
                        params.add(iBoundNode);
                    }
                }
                var defaultConstructor = MethodSearch.findConstructor(IOpenClass.EMPTY, bindingContext, type);
                if (isAllParamsAssign && duplicatedParamSyntaxNode != null) {
                    cleanErrorsAndMessages(bindingContext);
                    return ANodeBinder.makeErrorNode(String.format("Field '%s' has already used.",
                        duplicatedParamSyntaxNode.getText()), duplicatedParamSyntaxNode, bindingContext);
                } else if (isAllParamsAssign && defaultConstructor == null) {
                    cleanErrorsAndMessages(bindingContext);
                    return ANodeBinder.makeErrorNode(String.format("Default constructor is not found in type '%s'.",
                        type.getDisplayName(INamedThing.SHORT)), node, bindingContext);
                } else if (defaultConstructor != null && isAllParamsAssign) {
                    for (var e : namedParams.entrySet()) {
                        var f = type.getField(e.getKey());
                        if (f == null || f.isStatic() || !f.isWritable()) {
                            cleanErrorsAndMessages(bindingContext);
                            if (f == null) {
                                return ANodeBinder.makeErrorNode(String.format("Field '%s' is not found.", e.getKey()),
                                    e.getValue(),
                                    bindingContext);
                            }
                            if (f.isStatic()) {
                                return ANodeBinder.makeErrorNode(
                                    String.format("Field '%s' is found, but it is declared with static modifier.",
                                        e.getKey()),
                                    e.getValue(),
                                    bindingContext);
                            }
                            if (!f.isWritable()) {
                                return ANodeBinder.makeErrorNode(
                                    String.format("Field '%s' is found, but it is read only.", e.getKey()),
                                    e.getValue(),
                                    bindingContext);
                            }
                        }
                    }
                    var methodBoundNode = new MethodBoundNode(node, defaultConstructor);
                    return new ConstructorNamedParamsNode(localVar, methodBoundNode, params.toArray(IBoundNode.EMPTY));
                } else if (isAllParamsNoAssign && !Date.class.getName().equals(type.getName())) {
                    var children = params.toArray(IBoundNode.EMPTY);
                    var types = ANodeBinder.getTypes(children);
                    var methodCaller = MethodSearch.findConstructor(types, bindingContext, type);
                    if (methodCaller != null) {
                        BindHelper.checkOnDeprecation(node, bindingContext, methodCaller);
                        return new ConstructorParamsNode(
                            new MethodBoundNode(node, methodCaller, params.toArray(IBoundNode.EMPTY)));
                    }
                }
            } finally {
                bindingContext.popLocalVarContext();
            }
            cleanErrorsAndMessages(bindingContext);
            return null;
        } finally {
            bindingContext.popErrors().forEach(bindingContext::addError);
            bindingContext.popMessages().forEach(bindingContext::addMessage);
        }
    }

    private static void cleanErrorsAndMessages(IBindingContext bindingContext) {
        bindingContext.popErrors();
        bindingContext.popMessages();
        bindingContext.pushErrors();
        bindingContext.pushMessages();
    }

}
