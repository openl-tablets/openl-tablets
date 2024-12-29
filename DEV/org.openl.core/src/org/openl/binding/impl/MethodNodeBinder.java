package org.openl.binding.impl;

import java.util.Objects;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.openl.binding.IBindingContext;
import org.openl.binding.IBoundNode;
import org.openl.binding.MethodUtil;
import org.openl.binding.exception.MethodNotFoundException;
import org.openl.binding.impl.module.ModuleSpecificOpenField;
import org.openl.binding.impl.module.ModuleSpecificOpenMethod;
import org.openl.binding.impl.module.ModuleSpecificType;
import org.openl.binding.impl.module.WrapModuleSpecificTypes;
import org.openl.message.OpenLMessagesUtils;
import org.openl.syntax.ISyntaxNode;
import org.openl.syntax.impl.ISyntaxConstants;
import org.openl.syntax.impl.IdentifierNode;
import org.openl.types.IMethodCaller;
import org.openl.types.IOpenClass;

/**
 * @author snshor, Yury Molchan
 */
public class MethodNodeBinder extends ANodeBinder {

    private final Logger log = LoggerFactory.getLogger(MethodNodeBinder.class);

    protected IMethodCaller processFoundMethodCaller(IMethodCaller methodCaller) {
        return methodCaller;
    }

    @Override
    public IBoundNode bind(ISyntaxNode node, IBindingContext bindingContext) throws Exception {
        IBoundNode errorNode = validateNode(node, bindingContext);
        if (errorNode != null) {
            return errorNode;
        }

        var childrenCount = node.getNumberOfChildren();

        var funcNode = node.getChild(childrenCount - 1);
        var methodName = ((IdentifierNode) funcNode).getIdentifier();
        var parameterTypes = IOpenClass.EMPTY;

        bindingContext.pushErrors();
        bindingContext.pushMessages();
        boolean errorsAndMessagesPopped = false;
        try {
            var children = bindChildren(node, bindingContext, 0, childrenCount - 1);
            var syntaxNodeExceptions = bindingContext.popErrors();
            var openLMessages = bindingContext.popMessages();
            errorsAndMessagesPopped = true;

            if (syntaxNodeExceptions.isEmpty()) {

                parameterTypes = getTypes(children);

                var methodCaller = bindingContext
                        .findMethodCaller(ISyntaxConstants.THIS_NAMESPACE, methodName, parameterTypes);
                BindHelper.checkOnDeprecation(node, bindingContext, methodCaller);
                if (methodCaller != null) {
                    methodCaller = processFoundMethodCaller(methodCaller);
                    bindingContext.addMessages(openLMessages);
                    log(methodName, parameterTypes, "entirely appropriate by signature method");
                    return new MethodBoundNode(node, methodCaller, children);
                }

                // can`t find directly the method with given name and parameters. so,
                // if there are any parameters, try to bind it some additional ways
                // someMethod( parameter1, ... )
                //
                if (childrenCount > 1) {
                    // Get the root component type and dimension of the array.
                    IOpenClass argumentType = parameterTypes[0];
                    int dims = 0;
                    while (argumentType.isArray()) {
                        dims++;
                        argumentType = argumentType.getComponentClass();
                    }
                    IBoundNode field = bindAsFieldBoundNode(node,
                            methodName,
                            parameterTypes,
                            children,
                            childrenCount,
                            argumentType,
                            dims,
                            bindingContext);
                    if (field != null) {
                        bindingContext.addMessages(openLMessages);
                        return field;
                    }
                }
            }

            var type = bindingContext.findType(methodName);
            var childNodes = new ISyntaxNode[node.getNumberOfChildren() - 1];
            for (int i = 0; i < childNodes.length; i++) {
                childNodes[i] = node.getChild(i);
            }
            var iBoundNode = Optional.ofNullable(type)
                    .map(t -> ConstructorSugarSupport.makeSugarConstructor(node, childNodes, bindingContext, t, funcNode));
            if (iBoundNode.isPresent()) {
                return iBoundNode.get();
            }

            bindingContext.addMessages(openLMessages);
            if (!syntaxNodeExceptions.isEmpty()) {
                syntaxNodeExceptions.forEach(bindingContext::addError);
                return new ErrorBoundNode(node);
            }

            throw new MethodNotFoundException(methodName, parameterTypes);
        } finally {
            if (!errorsAndMessagesPopped) {
                bindingContext.popErrors();
                bindingContext.popMessages();
            }
        }
    }

    protected FieldBoundNode bindAsFieldBoundNode(ISyntaxNode methodNode,
                                                  String methodName,
                                                  IOpenClass[] argumentTypes,
                                                  IBoundNode[] children,
                                                  int childrenCount,
                                                  IOpenClass argumentType,
                                                  int dims,
                                                  IBindingContext bindingContext) throws Exception {
        // Try to bind method call Name(driver) as driver.Name;
        //
        if (childrenCount == 2) {
            // only one child, as there are 2 nodes, one of them is the function itself.
            //
            var field = argumentType.getField(methodName, false);
            if (field != null) {
                if (!Objects.equals(field.getName(), methodName)) {
                    bindingContext.addMessage(OpenLMessagesUtils
                            .newWarnMessage(String.format("Case insensitive matching to '%s'.", methodName), methodNode));
                }
                if (argumentType instanceof WrapModuleSpecificTypes && field.getType() instanceof ModuleSpecificType) {
                    var t = bindingContext.findType(field.getType().getName());
                    if (t != null) {
                        field = new ModuleSpecificOpenField(field, t);
                    }
                }
                log(methodName, argumentTypes, "field access method");
                return new FieldBoundNode(methodNode, field, children[0], dims);
            }
        }
        return null;
    }

    private void log(String methodName, IOpenClass[] argumentTypes, String bindingType) {
        if (log.isTraceEnabled()) {
            var method = MethodUtil.printMethod(methodName, argumentTypes);
            log.trace("Method {} has been binded as {}.", method, bindingType);
        }
    }

    @Override
    public IBoundNode bindTarget(ISyntaxNode node, IBindingContext bindingContext, IBoundNode target) throws Exception {

        var errorNode = validateNode(node, bindingContext);
        if (errorNode != null) {
            return errorNode;
        }

        var childrenCount = node.getNumberOfChildren();
        var lastNode = node.getChild(childrenCount - 1);

        var methodName = ((IdentifierNode) lastNode).getIdentifier();

        var children = bindChildren(node, bindingContext, 0, childrenCount - 1);
        var paramTypes = getTypes(children);

        var type = target.getType();
        var methodCaller = ModuleSpecificOpenMethod.findMethodCaller(type, methodName, paramTypes, bindingContext);
        BindHelper.checkOnDeprecation(node, bindingContext, methodCaller);

        if (methodCaller != null) {
            errorNode = validateMethod(node, bindingContext, target, methodCaller);
            if (errorNode != null) {
                return errorNode;
            }
            return new MethodBoundNode(node, target, methodCaller, children);
        }

        throw new MethodNotFoundException(type, methodName, paramTypes);
    }

    private IBoundNode validateMethod(ISyntaxNode node,
                                      IBindingContext bindingContext,
                                      IBoundNode target,
                                      IMethodCaller methodCaller) {
        boolean methodIsStatic = methodCaller.getMethod().isStatic();
        if (target.isStaticTarget() != methodIsStatic) {
            if (methodIsStatic) {
                BindHelper
                        .processWarn(String.format("Accessing to static method '%s' from non-static object of type '%s'.",
                                methodCaller.getMethod().getName(),
                                target.getType().getName()), node, bindingContext);
            } else {
                return makeErrorNode(String.format("Accessing to non-static method '%s' of static type '%s'.",
                        methodCaller.getMethod().getName(),
                        target.getType().getName()), node, bindingContext);
            }
        }
        return null;
    }

    private IBoundNode validateNode(ISyntaxNode node, IBindingContext bindingContext) {
        if (node.getNumberOfChildren() < 1) {
            return makeErrorNode("New node should have at least one subnode.", node, bindingContext);
        }
        return null;
    }
}
