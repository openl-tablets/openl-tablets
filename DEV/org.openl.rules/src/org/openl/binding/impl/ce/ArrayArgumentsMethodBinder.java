package org.openl.binding.impl.ce;

import java.util.List;

import org.openl.binding.IBoundNode;
import org.openl.rules.method.ITablePropertiesMethod;
import org.openl.rules.types.OpenMethodDispatcher;
import org.openl.syntax.ISyntaxNode;
import org.openl.types.IMethodCaller;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;

public class ArrayArgumentsMethodBinder extends org.openl.binding.impl.ArrayArgumentsMethodBinder {
    public ArrayArgumentsMethodBinder(String methodName, IOpenClass[] argumentsTypes, IBoundNode[] children) {
        super(methodName, argumentsTypes, children);
    }

    @Override
    protected IBoundNode makeMultiCallMethodBoundNode(ISyntaxNode node,
            IBoundNode[] children,
            List<Integer> arrayArgArgumentList,
            IMethodCaller singleParameterMethodCaller) {
        if (singleParameterMethodCaller.getMethod() instanceof ITablePropertiesMethod) {
            ITablePropertiesMethod tablePropertiesMethod = (ITablePropertiesMethod) singleParameterMethodCaller
                .getMethod();
            if (Boolean.TRUE.equals(tablePropertiesMethod.getMethodProperties().getParallel())) {
                return new MultiCallMethodBoundNodeMT(node,
                    children,
                    singleParameterMethodCaller,
                    arrayArgArgumentList);
            }
        }
        if (singleParameterMethodCaller.getMethod() instanceof OpenMethodDispatcher) {
            OpenMethodDispatcher openMethodDispatcher = (OpenMethodDispatcher) singleParameterMethodCaller.getMethod();
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
                return new MultiCallMethodBoundNodeMT(node,
                    children,
                    singleParameterMethodCaller,
                    arrayArgArgumentList);
            }
        }

        return new MultiCallMethodBoundNode(node, children, singleParameterMethodCaller, arrayArgArgumentList);
    }
}
