/*
 * Created on May 19, 2003 Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.binding.impl;

import java.lang.reflect.Method;

import org.openl.binding.IBindingContext;
import org.openl.binding.IBoundNode;
import org.openl.binding.MethodUtil;
import org.openl.binding.impl.cast.AutoCastFactory;
import org.openl.binding.impl.cast.AutoCastReturnType;
import org.openl.syntax.ISyntaxNode;
import org.openl.syntax.impl.ISyntaxConstants;
import org.openl.syntax.impl.IdentifierNode;
import org.openl.types.IMethodCaller;
import org.openl.types.IOpenClass;
import org.openl.types.impl.CastingMethodCaller;
import org.openl.types.java.JavaOpenMethod;
import org.openl.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author snshor
 */

public class MethodNodeBinder extends ANodeBinder {

    protected static final String FIELD_ACCESS_METHOD = "field access method";
    protected static final String VARIABLE_NUMBER_OF_ARGUMENTS_METHOD = "method with varialble number of arguments";
    protected static final String ARRAY_ARGUMENT_METHOD = "array argument method";
    protected static final String APPROPRIATE_BY_SIGNATURE_METHOD = "entirely appropriate by signature method";
    protected static final String NO_PARAMETERS = "no parameters";

    private final Logger log = LoggerFactory.getLogger(MethodNodeBinder.class);

    private IMethodCaller autoCastReturnTypeWrap(IBindingContext bindingContext,
            IMethodCaller methodCaller,
            IOpenClass[] parameterTypes) {

        JavaOpenMethod method = null;

        if (methodCaller instanceof CastingMethodCaller) {
            CastingMethodCaller castingMethodCaller = (CastingMethodCaller) methodCaller;
            if (castingMethodCaller.getMethod() instanceof JavaOpenMethod) {
                method = (JavaOpenMethod) castingMethodCaller.getMethod();
            }
        }

        if (methodCaller instanceof JavaOpenMethod) {
            method = (JavaOpenMethod) methodCaller;
        }

        if (method instanceof JavaOpenMethod) {
            JavaOpenMethod javaOpenMethod = (JavaOpenMethod) method;
            Method javaMethod = javaOpenMethod.getJavaMethod();
            AutoCastReturnType autoCastReturnType = javaMethod.getAnnotation(AutoCastReturnType.class);
            if (autoCastReturnType != null) {
                Class<? extends AutoCastFactory> clazz = autoCastReturnType.value();
                try {
                    AutoCastFactory autoCastFactory = clazz.newInstance();
                    return autoCastFactory.build(bindingContext, methodCaller, parameterTypes);
                } catch (InstantiationException e) {
                    return method;
                } catch (IllegalAccessException e) {
                    return method;
                }
            }
        }

        return methodCaller;
    }
    
    public IBoundNode bind(ISyntaxNode node, IBindingContext bindingContext) throws Exception {

        int childrenCount = node.getNumberOfChildren();

        if (childrenCount < 1) {
            BindHelper.processError("Method node should have at least one subnode", node, bindingContext, false);

            return new ErrorBoundNode(node);
        }

        ISyntaxNode lastNode = node.getChild(childrenCount - 1);

        String methodName = ((IdentifierNode) lastNode).getIdentifier();

        IBoundNode[] children = bindChildren(node, bindingContext, 0, childrenCount - 1);
        if (hasErrorBoundNode(children)){
            return new ErrorBoundNode(node);
        }
        IOpenClass[] parameterTypes = getTypes(children);

        IMethodCaller methodCaller = bindingContext.findMethodCaller(ISyntaxConstants.THIS_NAMESPACE,
            methodName,
            parameterTypes);

        methodCaller = autoCastReturnTypeWrap(bindingContext, methodCaller, parameterTypes);

        // can`t find directly the method with given name and parameters. so,
        // try to bind it some additional ways
        //
        if (methodCaller == null) {
            return bindWithAdditionalBinders(node, bindingContext, methodName, parameterTypes, children, childrenCount);
        }

        String bindingType = APPROPRIATE_BY_SIGNATURE_METHOD;
        log(methodName, parameterTypes, bindingType);
        return new MethodBoundNode(node, children, methodCaller);
    }

    protected IBoundNode makeArrayParametersMethod(ISyntaxNode methodNode,
            IBindingContext bindingContext,
            String methodName,
            IOpenClass[] argumentTypes,
            IBoundNode[] children) throws Exception {
        return new ArrayArgumentsMethodBinder(methodName, argumentTypes, children).bind(methodNode, bindingContext);
    }

    protected IBoundNode bindWithAdditionalBinders(ISyntaxNode methodNode,
            IBindingContext bindingContext,
            String methodName,
            IOpenClass[] argumentTypes,
            IBoundNode[] children,
            int childrenCount) throws Exception {

        // Try to bind method, that contains one of the arguments as array type.
        // For this try to find method without
        // array argument (but the component type of it on the same place). And
        // call it several times on runtime
        // for collecting results.
        //
        IBoundNode arrayParametersMethod = makeArrayParametersMethod(methodNode,
            bindingContext,
            methodName,
            argumentTypes,
            children);

        if (arrayParametersMethod != null) {
            String bindingType = ARRAY_ARGUMENT_METHOD;
            log(methodName, argumentTypes, bindingType);
            return arrayParametersMethod;
        }

        // Try to bind method call Name(driver) as driver.name;
        //
        if (childrenCount == 2) {
            IBoundNode accessorChain = new FieldAccessMethodBinder(methodName, children).bind(methodNode,
                bindingContext);
            if (accessorChain != null && !(accessorChain instanceof ErrorBoundNode)) {
                String bindingType = FIELD_ACCESS_METHOD;
                log(methodName, argumentTypes, bindingType);
                return accessorChain;
            }
        }

        // Try to bind method call as method with variable length of arguments
        //
        if (argumentTypes.length >= 1) {
            IBoundNode varArgsMethod = new VariableLengthArgumentsMethodBinder(methodName, argumentTypes, children)
                .bind(methodNode, bindingContext);
            if (varArgsMethod != null) {
                String bindingType = VARIABLE_NUMBER_OF_ARGUMENTS_METHOD;
                log(methodName, argumentTypes, bindingType);
                return varArgsMethod;
            }
        }

        return cantFindMethodError(methodNode, bindingContext, methodName, argumentTypes);
    }

    protected void log(String methodName, IOpenClass[] argumentTypes, String bindingType) {
        if (log.isTraceEnabled()) {
            log.trace("Method '{}' with parameters '{}' was binded as {}",
                methodName,
                getArgumentsAsString(argumentTypes),
                bindingType);
        }
    }

    private String getArgumentsAsString(IOpenClass[] argumentTypes) {
        String result = StringUtils.join(argumentTypes, ",");
        if (StringUtils.isNotBlank(result)) {
            return result;
        }
        return NO_PARAMETERS;
    }

    private IBoundNode cantFindMethodError(ISyntaxNode node,
            IBindingContext bindingContext,
            String methodName,
            IOpenClass[] parameterTypes) {

        String message = String.format("Method '%s' is not found", MethodUtil.printMethod(methodName, parameterTypes));
        BindHelper.processError(message, node, bindingContext, false);

        return new ErrorBoundNode(node);
    }

    @Override
    public IBoundNode bindTarget(ISyntaxNode node, IBindingContext bindingContext, IBoundNode target) throws Exception {

        int childrenCount = node.getNumberOfChildren();

        if (childrenCount < 1) {
            BindHelper.processError("New node should have at least one subnode", node, bindingContext, false);

            return new ErrorBoundNode(node);
        }

        ISyntaxNode lastNode = node.getChild(childrenCount - 1);

        String methodName = ((IdentifierNode) lastNode).getIdentifier();

        IBoundNode[] children = bindChildren(node, bindingContext, 0, childrenCount - 1);
        IOpenClass[] types = getTypes(children);

        IMethodCaller methodCaller = MethodSearch.getMethodCaller(methodName, types, bindingContext, target.getType());

        if (methodCaller == null) {

            StringBuilder buf = new StringBuilder("Method ");
            MethodUtil.printMethod(methodName, types, buf);
            buf.append(" is not found in '").append(target.getType().getName()).append("'");

            BindHelper.processError(buf.toString(), node, bindingContext, false);

            return new ErrorBoundNode(node);
        }

        if (target.isStaticTarget() != methodCaller.getMethod().isStatic()) {

            if (methodCaller.getMethod().isStatic()) {
                BindHelper.processWarn("Access of a static method from non-static object", node, bindingContext);
            } else {
                BindHelper.processError("Access of a non-static method from a static object", node, bindingContext, false);

                return new ErrorBoundNode(node);
            }
        }

        MethodBoundNode result = new MethodBoundNode(node, children, methodCaller, target);
        result.setTargetNode(target);

        return result;
    }

}
