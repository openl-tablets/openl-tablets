package org.openl.extension.xmlrules.binding;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.openl.binding.IBindingContext;
import org.openl.binding.IBoundNode;
import org.openl.binding.impl.*;
import org.openl.extension.xmlrules.ProjectData;
import org.openl.extension.xmlrules.model.Function;
import org.openl.extension.xmlrules.model.Table;
import org.openl.extension.xmlrules.model.single.node.IfErrorNode;
import org.openl.syntax.ISyntaxNode;
import org.openl.syntax.impl.ISyntaxConstants;
import org.openl.syntax.impl.IdentifierNode;
import org.openl.types.IMethodCaller;
import org.openl.types.IOpenClass;
import org.openl.types.java.JavaOpenClass;
import org.openl.types.java.JavaOpenMethod;

public class XmlRulesMethodNodeBinder extends MethodNodeBinder {
    @Override
    public IBoundNode bind(ISyntaxNode node, IBindingContext bindingContext) throws Exception {
        int childrenCount = node.getNumberOfChildren();

        if (childrenCount < 1) {
            BindHelper.processError("Method node should have at least one subnode", node, bindingContext, false);

            return new ErrorBoundNode(node);
        }

        ISyntaxNode lastNode = node.getChild(childrenCount - 1);

        String methodName = ((IdentifierNode) lastNode).getIdentifier();

        IBoundNode[] children = bindChildren(node, bindingContext, 0, childrenCount - 1);
        IOpenClass[] parameterTypes = getTypes(children);
        IOpenClass[] singleCallParameterTypes = new IOpenClass[parameterTypes.length];
        System.arraycopy(parameterTypes, 0, singleCallParameterTypes, 0, parameterTypes.length);

        IMethodCaller methodCaller = null;

        List<Integer> arrayCallArguments = getArrayCallArguments(children, parameterTypes);
        boolean isArrayCall = !arrayCallArguments.isEmpty();
        if (isArrayCall) {
            for (Integer arrayCallArgument : arrayCallArguments) {
                singleCallParameterTypes[arrayCallArgument] = parameterTypes[arrayCallArgument].getComponentClass();
            }

            methodCaller = bindingContext.findMethodCaller(ISyntaxConstants.THIS_NAMESPACE, methodName, parameterTypes);
            if (methodCaller == null) {
                methodCaller = bindingContext.findMethodCaller(ISyntaxConstants.THIS_NAMESPACE, methodName, singleCallParameterTypes);
            }

            boolean isAggregateFunction = methodCaller != null &&
                    (isReturnTwoDimensionArray(methodCaller) || paramsAreArrays(methodCaller, arrayCallArguments));
            if (isAggregateFunction) {
                methodCaller = null;
                isArrayCall = false;
            }
        }

        if (methodCaller == null) {
            methodCaller = bindingContext
                    .findMethodCaller(ISyntaxConstants.THIS_NAMESPACE, methodName, parameterTypes);
            isArrayCall = false;
        }


        // can`t find directly the method with given name and parameters. so, try to bind it some additional ways
        //
        if (methodCaller == null) {
            IBoundNode boundNode = null;
            if (!arrayCallArguments.isEmpty()) {
                boundNode = bindWithAdditionalBinders(node,
                        bindingContext,
                        methodName,
                        singleCallParameterTypes,
                        children,
                        childrenCount);
                if (boundNode instanceof MethodBoundNode) {
                    return new ArrayCallMethodBoundNode(node,
                            children,
                            ((MethodBoundNode) boundNode).getMethodCaller(),
                            arrayCallArguments);
                }
            }
            if (boundNode == null) {
                boundNode = bindWithAdditionalBinders(node,
                        bindingContext,
                        methodName,
                        parameterTypes,
                        children,
                        childrenCount);
            }

            return boundNode;
        }

        String bindingType = APPROPRIATE_BY_SIGNATURE_METHOD;
        log(methodName, parameterTypes, bindingType);
        if (isArrayCall) {
            return new ArrayCallMethodBoundNode(node, children, methodCaller, arrayCallArguments);
        }
        return new MethodBoundNode(node, children, methodCaller);
    }

    @Override
    protected IBoundNode bindWithAdditionalBinders(ISyntaxNode methodNode,
            IBindingContext bindingContext,
            String methodName,
            IOpenClass[] argumentTypes,
            IBoundNode[] children,
            int childrenCount) throws Exception {

        if (IfErrorNode.FUNCTION_NAME.equals(methodName) && children.length == IfErrorNode.ARGUMENTS_COUNT) {
            return new IfErrorFunctionBoundNode(methodNode, children);
        }

        IBoundNode methodWithModifiedAttributes = bindModifiedAttributes(methodNode,
                bindingContext,
                methodName,
                argumentTypes,
                children);

        if (methodWithModifiedAttributes != null) {
            return methodWithModifiedAttributes;
        }

        return super.bindWithAdditionalBinders(methodNode,
                bindingContext,
                methodName,
                argumentTypes,
                children,
                childrenCount);
    }

    private IBoundNode bindModifiedAttributes(ISyntaxNode methodNode,
            IBindingContext bindingContext,
            String methodName,
            IOpenClass[] argumentTypes, IBoundNode[] children) {
        IMethodCaller modifyContext = bindingContext.findMethodCaller(ISyntaxConstants.THIS_NAMESPACE,
                "modifyContext",
                new IOpenClass[] { JavaOpenClass.STRING, JavaOpenClass.OBJECT });
        IMethodCaller restoreContext = bindingContext.findMethodCaller(ISyntaxConstants.THIS_NAMESPACE,
                "restoreContext",
                new IOpenClass[] {});
        ProjectData instance = ProjectData.getCurrentInstance();

        for (Function function : instance.getFunctions()) {
            if (function.getName().equals(methodName)) {
                int parameterCount = function.getParameters().size();
                int possibleParameterCount = parameterCount + function.getAttributes().size();
                if (parameterCount < children.length && possibleParameterCount >= children.length) {
                    IOpenClass[] parameterTypes = Arrays.copyOfRange(argumentTypes, 0, parameterCount);
                    IMethodCaller methodCaller = bindingContext.findMethodCaller(ISyntaxConstants.THIS_NAMESPACE,
                            methodName, parameterTypes);
                    if (methodCaller == null) {
                        return null;
                    }

                    return new MethodWithAttributesBoundNode(methodNode,
                            children,
                            methodCaller,
                            modifyContext,
                            restoreContext,
                            function.getAttributes(),
                            parameterCount);
                } else {
                    return null;
                }
            }
        }

        for (Table table : instance.getTables()) {
            if (table.getName().equals(methodName)) {
                int parameterCount = table.getVerticalConditions().size() + table.getHorizontalConditions().size() + table.getParameters().size();
                int possibleParameterCount = parameterCount + table.getAttributes().size();
                if (parameterCount < children.length && possibleParameterCount >= children.length) {
                    IOpenClass[] parameterTypes = Arrays.copyOfRange(argumentTypes, 0, parameterCount);
                    IMethodCaller methodCaller = bindingContext.findMethodCaller(ISyntaxConstants.THIS_NAMESPACE,
                            methodName, parameterTypes);
                    if (methodCaller == null) {
                        return null;
                    }

                    return new MethodWithAttributesBoundNode(methodNode,
                            children,
                            methodCaller,
                            modifyContext,
                            restoreContext,
                            table.getAttributes(),
                            parameterCount);
                } else {
                    return null;
                }
            }
        }

        return null;
    }

    private List<Integer> getArrayCallArguments(IBoundNode[] children, IOpenClass[] parameterTypes) {
        List<Integer> arrayCallArguments = new ArrayList<Integer>();

        for (int i = 0; i < children.length; i++) {
            IBoundNode child = children[i];
            boolean isReturnsArray = child instanceof MethodBoundNode &&
                    isReturnTwoDimensionArray(((MethodBoundNode) child).getMethodCaller());
            if (isReturnsArray || child instanceof ArrayCallMethodBoundNode || child instanceof ArrayInitializerNode) {
                // Found array call argument
                IOpenClass parameterType = parameterTypes[i];
                if (!parameterType.isArray()) {
                    throw new IllegalArgumentException("Parameter " + i + " has a type " + parameterType.getName() + " but array is expected");
                }

                arrayCallArguments.add(i);
            }
        }
        return arrayCallArguments;
    }

    private boolean isReturnTwoDimensionArray(IMethodCaller methodCaller) {
        IOpenClass type = methodCaller.getMethod().getType();
        return type.isArray() && type.getComponentClass().isArray();
    }

    private boolean paramsAreArrays(IMethodCaller methodCaller, List<Integer> arrayCallArguments) {
        IOpenClass arrayCallParameter = methodCaller.getMethod().getSignature().getParameterType(arrayCallArguments.get(0));
        return arrayCallParameter.isArray() ||
                JavaOpenClass.OBJECT.equals(arrayCallParameter) && methodCaller instanceof JavaOpenMethod;
    }
}
