package org.openl.binding.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.openl.binding.IBindingContext;
import org.openl.binding.IBoundNode;
import org.openl.binding.MethodUtil;
import org.openl.binding.exception.FieldNotFoundException;
import org.openl.binding.exception.MethodNotFoundException;
import org.openl.binding.impl.cast.IOpenCast;
import org.openl.binding.impl.cast.OutsideOfValidDomainException;
import org.openl.binding.impl.method.MultiCallOpenMethod;
import org.openl.binding.impl.method.MultiCallOpenMethodMT;
import org.openl.binding.impl.module.ModuleSpecificOpenField;
import org.openl.binding.impl.module.ModuleSpecificOpenMethod;
import org.openl.binding.impl.module.ModuleSpecificType;
import org.openl.binding.impl.module.WrapModuleSpecificTypes;
import org.openl.message.OpenLMessagesUtils;
import org.openl.rules.calc.SpreadsheetResult;
import org.openl.rules.calc.SpreadsheetStructureBuilder;
import org.openl.rules.method.ITablePropertiesMethod;
import org.openl.rules.types.OpenMethodDispatcher;
import org.openl.syntax.ISyntaxNode;
import org.openl.syntax.impl.ISyntaxConstants;
import org.openl.syntax.impl.IdentifierNode;
import org.openl.types.IMethodCaller;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;
import org.openl.types.impl.DomainOpenClass;
import org.openl.util.DomainUtils;
import org.openl.util.StringUtils;

/**
 * @author snshor, Yury Molchan
 */
public class MethodNodeBinder extends ANodeBinder {

    private final Logger log = LoggerFactory.getLogger(MethodNodeBinder.class);

    protected IMethodCaller processFoundMethodCaller(IMethodCaller methodCaller) {
        if (methodCaller instanceof MultiCallOpenMethod multiCall && isParallel(multiCall.getSourceMethod())) {
            return new MultiCallOpenMethodMT(multiCall);
        }
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
        var argumentTypes = IOpenClass.EMPTY;

        bindingContext.pushErrors();
        bindingContext.pushMessages();
        boolean errorsAndMessagesPopped = false;
        try {
            var children = bindChildren(node, bindingContext, 0, childrenCount - 1);
            var syntaxNodeExceptions = bindingContext.popErrors();
            var openLMessages = bindingContext.popMessages();
            errorsAndMessagesPopped = true;

            if (syntaxNodeExceptions.isEmpty()) {

                argumentTypes = getTypes(children);

                var methodCaller = bindingContext
                        .findMethodCaller(ISyntaxConstants.THIS_NAMESPACE, methodName, argumentTypes);
                BindHelper.checkOnDeprecation(node, bindingContext, methodCaller);
                if (methodCaller != null) {
                    methodCaller = processFoundMethodCaller(methodCaller);
                    validateMethodArguments(methodCaller, children, node, bindingContext);
                    bindingContext.addMessages(openLMessages);
                    log(methodName, argumentTypes, "entirely appropriate by signature method");
                    return new MethodBoundNode(node, methodCaller, children);
                }

                // can`t find directly the method with given name and parameters. so,
                // if there are any parameters, try to bind it some additional ways
                // someMethod( parameter1, ... )
                //
                if (childrenCount > 1) {
                    // Get the root component type and dimension of the array.
                    IOpenClass argumentType = argumentTypes[0];
                    int dims = 0;
                    while (argumentType.isArray()) {
                        dims++;
                        argumentType = argumentType.getComponentClass();
                    }
                    IBoundNode field = bindAsFieldBoundNode(node,
                            methodName,
                            argumentTypes,
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

            throw new MethodNotFoundException(methodName, argumentTypes);
        } finally {
            if (!errorsAndMessagesPopped) {
                bindingContext.popErrors();
                bindingContext.popMessages();
            }
        }
    }

    private boolean isParallel(IOpenMethod openMethod) {
        boolean parallel = false;
        if (openMethod instanceof ITablePropertiesMethod) {
            ITablePropertiesMethod tablePropertiesMethod = (ITablePropertiesMethod) openMethod.getMethod();
            if (Boolean.TRUE.equals(tablePropertiesMethod.getMethodProperties().getParallel())) {
                parallel = true;
            }
        }
        if (openMethod instanceof OpenMethodDispatcher openMethodDispatcher) {
            boolean f = true;
            for (IOpenMethod method : openMethodDispatcher.getCandidates()) {
                if (method instanceof ITablePropertiesMethod tablePropertiesMethod) {
                    if (!Boolean.TRUE.equals(tablePropertiesMethod.getMethodProperties().getParallel())) {
                        f = false;
                        break;
                    }
                } else {
                    f = false;
                    break;
                }
            }
            if (f) {
                parallel = true;
            }
        }
        return parallel;
    }

    private void validateMethodArguments(IMethodCaller methodCaller,
                                         IBoundNode[] methodArguments,
                                         ISyntaxNode methodInvocationNode,
                                         IBindingContext bindingContext) {
        var parameterTypes = methodCaller.getMethod().getSignature().getParameterTypes();
        var childrenAmount = methodArguments.length;
        var parametersAmount = Math.min(parameterTypes.length, childrenAmount);
        for (var index = 0; index < parametersAmount; index++) {
            validateArgument(methodArguments[index], parameterTypes[index], methodInvocationNode, bindingContext);
        }
        // In case if last parameter is var args
        if (childrenAmount > parametersAmount) {
            for (var j = parametersAmount; j < childrenAmount; j++) {
                validateArgument(methodArguments[j], parameterTypes[parametersAmount - 1], methodInvocationNode, bindingContext);
            }
        }
    }

    private void validateArgument(IBoundNode methodArgumentNode,
                                  IOpenClass parameterType,
                                  ISyntaxNode methodInvocationNode,
                                  IBindingContext bindingContext) {
        if (parameterType instanceof DomainOpenClass && canBeValidated(methodArgumentNode)) {
            if (containsLiteralValue(methodArgumentNode)) {
                if (parameterType.isArray()) {
                    validateArgumentForArrayParameter(methodArgumentNode, parameterType, methodInvocationNode, bindingContext);
                } else {
                    validateArgumentForLiteralParameter(methodArgumentNode, parameterType, methodInvocationNode, bindingContext);
                }
            }
        }
    }

    private boolean canBeValidated(IBoundNode methodArgumentNode) {
        return methodArgumentNode instanceof LiteralBoundNode
                || methodArgumentNode instanceof ArrayInitializerNode
                || methodArgumentNode instanceof ConstructorNamedParamsNode;
    }

    private boolean containsLiteralValue(IBoundNode methodArgumentNode) {
        if (methodArgumentNode instanceof LiteralBoundNode) {
            return true;
        }

        IBoundNode[] children = methodArgumentNode.getChildren();
        for (IBoundNode child : children) {
            if (containsLiteralValue(child)) {
                return true;
            }
        }

        return false;
    }

    private void validateArgumentForLiteralParameter(IBoundNode methodArgumentNode,
                                                     IOpenClass parameterType,
                                                     ISyntaxNode methodInvocationNode,
                                                     IBindingContext bindingContext) {
        if (methodArgumentNode instanceof LiteralBoundNode literalBoundNode) {
            tryCastLiteralArgument(literalBoundNode, parameterType, methodInvocationNode, bindingContext);
        } else {
            for (IBoundNode child : methodArgumentNode.getChildren()) {
                validateArgumentForLiteralParameter(child, parameterType, methodInvocationNode, bindingContext);
            }
        }
    }

    private void validateArgumentForArrayParameter(IBoundNode methodArgumentNode,
                                                   IOpenClass parameterType,
                                                   ISyntaxNode methodInvocationNode,
                                                   IBindingContext bindingContext) {
        // TODO: 1. count dim for parameter type
        //  2. get argument on the same dim
        //  convert
        if (methodArgumentNode instanceof LiteralBoundNode literalBoundNode) {
            tryCastLiteralArgument(literalBoundNode, parameterType, methodInvocationNode, bindingContext);
        } else if (methodArgumentNode.getChildren().length > 0) {
            if (methodArgumentNode.getChildren()[0] instanceof LiteralBoundNode) { // if we reached array of literals
                tryCastArrayArgument(methodArgumentNode, parameterType, methodInvocationNode, bindingContext);
            } else if (!(methodArgumentNode.getChildren()[0] instanceof LiteralBoundNode)) { // if an array wrapped by other nodes
                for (IBoundNode argumentNodeChild : methodArgumentNode.getChildren()) {
                    validateArgumentForArrayParameter(argumentNodeChild, parameterType, methodInvocationNode, bindingContext);
                }
            }
        } else {
            // passed argument doesn't have literal value to validate
//            throw new RuntimeException("Passed argument doesn't have literal value to validate though I'll keep the exception so far");
        }
    }

    private void tryCastArrayArgument(IBoundNode methodArgumentNode, IOpenClass parameterType, ISyntaxNode methodInvocationNode, IBindingContext bindingContext) {
        Object[] values = buildArrayOfArgumentValues(methodArgumentNode);
        try {
            IOpenCast methodParameterCast = getCast(methodArgumentNode, parameterType, bindingContext);
            if (methodParameterCast != null) {
                methodParameterCast.convert(values);
            }
        } catch (OutsideOfValidDomainException e) {
            BindHelper.processError(String.format("Object '%s' is outside of a valid domain '%s'. Valid values: %s",
                            StringUtils.join(values, ","),
                            parameterType,
                            DomainUtils.toString(parameterType.getDomain())),
                    methodInvocationNode, bindingContext);
        } catch (TypeCastException e) {
            BindHelper.processError(String.format("An error occurred while casting an argument '%s' into '%s': %s",
                            StringUtils.join(values, ","),
                            parameterType,
                            e.getMessage()),
                    methodInvocationNode, bindingContext);
        }
    }

    private void tryCastLiteralArgument(LiteralBoundNode literalArgumentNode,
                                        IOpenClass parameterType,
                                        ISyntaxNode methodInvocationNode,
                                        IBindingContext bindingContext) {
        try {
            IOpenCast methodParameterCast = getCast(literalArgumentNode, parameterType, bindingContext);
            if (methodParameterCast != null) {
                methodParameterCast.convert(literalArgumentNode.getValue());
            }
        } catch (OutsideOfValidDomainException exception) {
            BindHelper.processError(String.format("Object '%s' is outside of a valid domain '%s'. Valid values: %s",
                            literalArgumentNode.getValue(),
                            parameterType,
                            DomainUtils.toString(parameterType.getDomain())),
                    methodInvocationNode, bindingContext);
        } catch (TypeCastException e) {
            BindHelper.processError(String.format("An error occurred while casting an argument '%s' into '%s': %s",
                            literalArgumentNode.getValue(),
                            parameterType,
                            e.getMessage()),
                    methodInvocationNode, bindingContext);
        }
    }

    private Object[] buildArrayOfArgumentValues(IBoundNode methodArgumentNode) {
        var objects = new ArrayList<>();
        for (IBoundNode child : methodArgumentNode.getChildren()) {
            collectAllLiteralValues(child, objects);
        }

        return objects.toArray();
    }

    private void collectAllLiteralValues(IBoundNode node, List<Object> objects) {
        if (node instanceof LiteralBoundNode literalBoundNode) {
            objects.add(literalBoundNode.getValue());
        } else {
            if (node.getChildren() != null) {
                node.getChildren();
                for (IBoundNode nodeChild : node.getChildren()) {
                    collectAllLiteralValues(nodeChild, objects);
                }
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
        if (methodName
                .startsWith(SpreadsheetStructureBuilder.DOLLAR_SIGN) && SpreadsheetResult.class
                .equals(argumentType.getInstanceClass())) {
            throw new FieldNotFoundException("", methodName, argumentType);
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
    public IBoundNode bindTarget(ISyntaxNode node, IBindingContext bindingContext, IBoundNode target) {

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
