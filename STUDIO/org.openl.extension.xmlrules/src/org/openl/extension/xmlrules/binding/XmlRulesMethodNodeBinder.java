package org.openl.extension.xmlrules.binding;

import java.util.Arrays;

import org.openl.binding.IBindingContext;
import org.openl.binding.IBoundNode;
import org.openl.binding.impl.MethodNodeBinder;
import org.openl.extension.xmlrules.ProjectData;
import org.openl.extension.xmlrules.model.Function;
import org.openl.extension.xmlrules.model.Table;
import org.openl.extension.xmlrules.model.single.node.IfErrorNode;
import org.openl.syntax.ISyntaxNode;
import org.openl.syntax.impl.ISyntaxConstants;
import org.openl.types.IMethodCaller;
import org.openl.types.IOpenClass;
import org.openl.types.java.JavaOpenClass;

public class XmlRulesMethodNodeBinder extends MethodNodeBinder {
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
}
