package org.openl.extension.xmlrules.binding;

import org.openl.binding.IBindingContext;
import org.openl.binding.IBoundNode;
import org.openl.binding.impl.MethodNodeBinder;
import org.openl.extension.xmlrules.model.single.node.IfErrorNode;
import org.openl.syntax.ISyntaxNode;
import org.openl.types.IOpenClass;

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

        return super.bindWithAdditionalBinders(methodNode,
                bindingContext,
                methodName,
                argumentTypes,
                children,
                childrenCount);
    }
}
