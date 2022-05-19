package org.openl.binding.impl.ce;

import org.openl.binding.IBindingContext;
import org.openl.binding.IBoundNode;
import org.openl.binding.exception.FieldNotFoundException;
import org.openl.binding.impl.FieldBoundNode;
import org.openl.binding.impl.method.AOpenMethodDelegator;
import org.openl.binding.impl.method.MultiCallOpenMethod;
import org.openl.binding.impl.method.MultiCallOpenMethodMT;
import org.openl.rules.calc.SpreadsheetResult;
import org.openl.rules.calc.SpreadsheetStructureBuilder;
import org.openl.rules.method.ITablePropertiesMethod;
import org.openl.rules.types.OpenMethodDispatcher;
import org.openl.syntax.ISyntaxNode;
import org.openl.types.IMethodCaller;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;

public class MethodNodeBinder extends org.openl.binding.impl.MethodNodeBinder {

    private IOpenMethod extractMethod(IOpenMethod openMethod) {
        if (openMethod instanceof AOpenMethodDelegator) {
            return extractMethod(((AOpenMethodDelegator) openMethod).getDelegate());
        }
        return openMethod;
    }

    @Override
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

    @Override
    protected FieldBoundNode bindAsFieldBoundNode(ISyntaxNode methodNode,
            String methodName,
            IOpenClass[] argumentTypes,
            IBoundNode[] children,
            int childrenCount,
            IOpenClass argumentType,
            int dims,
            IBindingContext bindingContext) throws Exception {
        FieldBoundNode fieldBoundNode = super.bindAsFieldBoundNode(methodNode,
            methodName,
            argumentTypes,
            children,
            childrenCount,
            argumentType,
            dims,
            bindingContext);
        if (fieldBoundNode == null && methodName
            .startsWith(SpreadsheetStructureBuilder.DOLLAR_SIGN) && SpreadsheetResult.class
                .equals(argumentType.getInstanceClass())) {
            throw new FieldNotFoundException("", methodName, argumentType);
        }
        return fieldBoundNode;
    }
}
