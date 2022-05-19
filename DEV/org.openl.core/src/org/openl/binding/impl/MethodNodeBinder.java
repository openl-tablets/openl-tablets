package org.openl.binding.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.openl.binding.IBindingContext;
import org.openl.binding.IBoundNode;
import org.openl.binding.ILocalVar;
import org.openl.binding.MethodUtil;
import org.openl.binding.exception.MethodNotFoundException;
import org.openl.binding.impl.method.MethodSearch;
import org.openl.binding.impl.module.ModuleSpecificOpenField;
import org.openl.binding.impl.module.ModuleSpecificOpenMethod;
import org.openl.binding.impl.module.ModuleSpecificType;
import org.openl.binding.impl.module.WrapModuleSpecificTypes;
import org.openl.message.OpenLMessage;
import org.openl.syntax.ISyntaxNode;
import org.openl.syntax.exception.SyntaxNodeException;
import org.openl.syntax.impl.ISyntaxConstants;
import org.openl.syntax.impl.IdentifierNode;
import org.openl.types.IMethodCaller;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author snshor, Yury Molchan
 */
public class MethodNodeBinder extends ANodeBinder {

    private final Logger log = LoggerFactory.getLogger(MethodNodeBinder.class);

    @Override
    public IBoundNode bind(ISyntaxNode node, IBindingContext bindingContext) throws Exception {

        IBoundNode errorNode = validateNode(node, bindingContext);
        if (errorNode != null) {
            return errorNode;
        }

        int childrenCount = node.getNumberOfChildren();

        ISyntaxNode lastNode = node.getChild(childrenCount - 1);

        String methodName = ((IdentifierNode) lastNode).getIdentifier();
        IOpenClass[] parameterTypes = IOpenClass.EMPTY;

        bindingContext.pushErrors();
        bindingContext.pushMessages();
        IBoundNode[] children = bindChildren(node, bindingContext, 0, childrenCount - 1);
        List<SyntaxNodeException> syntaxNodeExceptions = bindingContext.popErrors();
        Collection<OpenLMessage> openLMessages = bindingContext.popMessages();

        if (syntaxNodeExceptions.isEmpty()) {

            parameterTypes = getTypes(children);

            IMethodCaller methodCaller = bindingContext
                .findMethodCaller(ISyntaxConstants.THIS_NAMESPACE, methodName, parameterTypes);

            BindHelper.checkOnDeprecation(node, bindingContext, methodCaller);

            if (methodCaller != null) {
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

        IOpenClass type = bindingContext.findType(ISyntaxConstants.THIS_NAMESPACE, methodName);
        Optional<IBoundNode> iBoundNode = Optional.ofNullable(type)
            .map(t -> makeDatatypeConstructor(node, bindingContext, t));
        if (iBoundNode.isPresent()) {
            return iBoundNode.get();
        }

        bindingContext.addMessages(openLMessages);
        if (!syntaxNodeExceptions.isEmpty()) {
            syntaxNodeExceptions.forEach(bindingContext::addError);
            return new ErrorBoundNode(node);
        }

        throw new MethodNotFoundException(methodName, parameterTypes);
    }

    private IBoundNode makeDatatypeConstructor(ISyntaxNode node, IBindingContext bindingContext, IOpenClass type) {
        List<IBoundNode> params = new ArrayList<>();
        List<String> namedParams = new ArrayList<>();
        boolean isAllParamsAssign = true;
        try {
            bindingContext.pushLocalVarContext();
            ILocalVar localVar = bindingContext
                .addVar(ISyntaxConstants.THIS_NAMESPACE, bindingContext.getTemporaryVarName(), type);
            TypeBindingContext varBindingContext = TypeBindingContext.create(bindingContext, localVar, 1);
            for (int i = 0; i < node.getNumberOfChildren() - 1; i++) {
                ISyntaxNode child = node.getChild(i);
                String childType = child.getType();
                if ("op.assign".equals(childType)) {
                    IBoundNode iBoundNode = bindChildNode(child, varBindingContext);
                    String paramName = child.getChild(0).getText();
                    if (namedParams.contains(paramName)) {
                        isAllParamsAssign = false;
                    }
                    namedParams.add(paramName);
                    params.add(iBoundNode);
                } else {
                    isAllParamsAssign = false;
                    IBoundNode iBoundNode = bindChildNode(child, bindingContext);
                    params.add(iBoundNode);
                }
            }
            if (isAllParamsAssign && namedParams.stream().allMatch(n -> type.getField(n) != null)) {
                IMethodCaller methodCaller = MethodSearch.findConstructor(IOpenClass.EMPTY, bindingContext, type);
                MethodBoundNode methodBoundNode = new MethodBoundNode(node, methodCaller);
                return new ConstructorNamedParamsNode(localVar, methodBoundNode, params.toArray(IBoundNode.EMPTY));
            } else if (!Date.class.getName().equals(type.getName())) {
                IBoundNode[] children = params.toArray(IBoundNode.EMPTY);
                if (hasErrorBoundNode(children)) {
                    return new ErrorBoundNode(node);
                }
                IOpenClass[] types = getTypes(children);
                IMethodCaller methodCaller = MethodSearch.findConstructor(types, bindingContext, type);

                if (methodCaller != null) {
                    BindHelper.checkOnDeprecation(node, bindingContext, methodCaller);
                    return new ConstructorParamsNode(
                        new MethodBoundNode(node, methodCaller, params.toArray(IBoundNode.EMPTY)));
                }

            }
        } finally {
            bindingContext.popLocalVarContext();
        }
        return null;
    }

    private boolean isAllowParallelInvocation(IMethodCaller singleParameterMethodCaller) {
        return false;
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
            IOpenField field = argumentType.getField(methodName, false);
            if (field != null) {
                if (argumentType instanceof WrapModuleSpecificTypes && field.getType() instanceof ModuleSpecificType) {
                    IOpenClass t = bindingContext.findType(ISyntaxConstants.THIS_NAMESPACE, field.getType().getName());
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
            String method = MethodUtil.printMethod(methodName, argumentTypes);
            log.trace("Method {} has been binded as {}.", method, bindingType);
        }
    }

    @Override
    public IBoundNode bindTarget(ISyntaxNode node, IBindingContext bindingContext, IBoundNode target) throws Exception {

        IBoundNode errorNode = validateNode(node, bindingContext);
        if (errorNode != null) {
            return errorNode;
        }

        int childrenCount = node.getNumberOfChildren();
        ISyntaxNode lastNode = node.getChild(childrenCount - 1);

        String methodName = ((IdentifierNode) lastNode).getIdentifier();

        IBoundNode[] children = bindChildren(node, bindingContext, 0, childrenCount - 1);
        IOpenClass[] paramTypes = getTypes(children);

        IOpenClass type = target.getType();
        IMethodCaller methodCaller = ModuleSpecificOpenMethod
            .findMethodCaller(type, methodName, paramTypes, bindingContext);
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
