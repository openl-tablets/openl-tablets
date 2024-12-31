package org.openl.binding.impl;

import java.util.Objects;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.openl.binding.IBindingContext;
import org.openl.binding.IBoundNode;
import org.openl.binding.MethodUtil;
import org.openl.binding.exception.FieldNotFoundException;
import org.openl.binding.exception.MethodNotFoundException;
import org.openl.binding.impl.method.AOpenMethodDelegator;
import org.openl.binding.impl.method.MultiCallOpenMethod;
import org.openl.binding.impl.method.MultiCallOpenMethodMT;
import org.openl.binding.impl.module.ModuleSpecificOpenField;
import org.openl.binding.impl.module.ModuleSpecificOpenMethod;
import org.openl.binding.impl.module.ModuleSpecificType;
import org.openl.binding.impl.module.WrapModuleSpecificTypes;
import org.openl.domain.EnumDomain;
import org.openl.domain.IDomain;
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
import org.openl.util.DomainUtils;
import org.openl.util.OpenClassUtils;

/**
 * @author snshor, Yury Molchan
 */
public class MethodNodeBinder extends ANodeBinder {

    private final Logger log = LoggerFactory.getLogger(MethodNodeBinder.class);

    private IOpenMethod extractMethod(IOpenMethod openMethod) {
        if (openMethod instanceof AOpenMethodDelegator) {
            return extractMethod(((AOpenMethodDelegator) openMethod).getDelegate());
        }
        return openMethod;
    }

    protected IMethodCaller processFoundMethodCaller(IMethodCaller methodCaller) {
        if (methodCaller instanceof MultiCallOpenMethod) {
            IOpenMethod openMethod = extractMethod(methodCaller.getMethod());
            if (isParallel(openMethod)) {
                MultiCallOpenMethod multiCallOpenMethod = (MultiCallOpenMethod) methodCaller;
                return new MultiCallOpenMethodMT((multiCallOpenMethod));
            }
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
                    validateMethodParameters(methodCaller, children, node, bindingContext);
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
                    .map(t -> ConstructorSugarSupport.makeSugarConstructor(node,
                            childNodes,
                            bindingContext,
                            t,
                            funcNode));
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

    private boolean isParallel(IOpenMethod openMethod) {
        boolean parallel = false;
        if (openMethod instanceof ITablePropertiesMethod) {
            ITablePropertiesMethod tablePropertiesMethod = (ITablePropertiesMethod) openMethod.getMethod();
            if (Boolean.TRUE.equals(tablePropertiesMethod.getMethodProperties().getParallel())) {
                parallel = true;
            }
        }
        if (openMethod instanceof OpenMethodDispatcher) {
            OpenMethodDispatcher openMethodDispatcher = (OpenMethodDispatcher) openMethod;
            boolean f = true;
            for (IOpenMethod method : openMethodDispatcher.getCandidates()) {
                if (method instanceof ITablePropertiesMethod) {
                    ITablePropertiesMethod tablePropertiesMethod = (ITablePropertiesMethod) method;
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

    private void validateMethodParameters(IMethodCaller methodCaller,
                                          IBoundNode[] children,
                                          ISyntaxNode node,
                                          IBindingContext bindingContext) {
        var parameterTypes = methodCaller.getMethod().getSignature().getParameterTypes();
        var noOfChildren = children.length;
        var parameterCount = Math.min(parameterTypes.length, noOfChildren);
        for (var i = 0; i < parameterCount; i++) {
            validateParam(parameterTypes[i], children[i], node, bindingContext);
        }
        // In case of last parameter var args
        if (noOfChildren > parameterCount) {
            for (var j = parameterCount; j < noOfChildren; j++) {
                validateParam(parameterTypes[parameterCount - 1], children[j], node, bindingContext);
            }
        }
    }

    private void validateParam(IOpenClass parameterType,
                               IBoundNode param,
                               ISyntaxNode node,
                               IBindingContext bindingContext) {
        var domain = parameterType.getDomain();
        if (domain != null) {
            var iDomain = (IDomain<Object>) domain;
            if (param instanceof LiteralBoundNode) {
                processLiteralBoundNode(((LiteralBoundNode) param).getValue(),
                        iDomain,
                        node,
                        bindingContext,
                        parameterType.getName());
            } else if (param instanceof ArrayInitializerNode) {
                // In case of MultiCallOpenMethod or enum is an array
                validateParameterArray(((ArrayInitializerNode) param).children,
                        iDomain,
                        node,
                        bindingContext,
                        parameterType.getName());
            }
        }
    }

    private void processLiteralBoundNode(Object inputValue,
                                         IDomain<Object> domain,
                                         ISyntaxNode node,
                                         IBindingContext bindingContext,
                                         String toClass) {
        if (inputValue != null && !domain.selectObject(inputValue)) {
            BindHelper.processError(String.format("Object '%s' is outside of valid domain '%s'. Valid values: %s",
                    inputValue,
                    toClass,
                    DomainUtils.toString(domain)), node, bindingContext);
        }
    }

    private void validateParameterArray(IBoundNode[] iBoundNode,
                                        IDomain<Object> domain,
                                        ISyntaxNode node,
                                        IBindingContext bindingContext,
                                        String toClass) {
        var enumDomain = (EnumDomain<Object>) domain;
        if (enumDomain.getComponentType().isArray() && iBoundNode[0] instanceof LiteralBoundNode) {
            // Enum/domain is itself an array
            var stringBuilder = new StringBuilder();
            var inputkey = generateEnumKey(stringBuilder, iBoundNode);
            if (inputkey != null && !OpenClassUtils.belongsToEnum(enumDomain.getAllObjects(), inputkey)) {
                BindHelper.processError(String.format("Object '%s' is outside of valid domain '%s'. Valid values: %s",
                        inputkey,
                        toClass,
                        DomainUtils.toString(enumDomain)), node, bindingContext);
            }
        } else {
            for (IBoundNode boundNode : iBoundNode) {
                if (boundNode instanceof LiteralBoundNode) {
                    // MultiCallOpenMethod
                    processLiteralBoundNode(((LiteralBoundNode) boundNode).getValue(),
                            domain,
                            node,
                            bindingContext,
                            toClass);
                } else {
                    validateParameterArray(boundNode.getChildren(), domain, node, bindingContext, toClass);
                }

            }
        }
    }

    private String generateEnumKey(StringBuilder enumKey, IBoundNode[] iBoundNodes) {
        for (IBoundNode boundNode : iBoundNodes) {
            LiteralBoundNode literalBoundNode = (LiteralBoundNode) boundNode;
            if (literalBoundNode.getValue() != null) {
                enumKey.append(literalBoundNode.getValue()).append(",");
            }
        }
        var enumKeyLen = enumKey.length();
        return enumKeyLen > 0 ? enumKey.substring(0, enumKeyLen - 1) : null;
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
