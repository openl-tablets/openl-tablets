package org.openl.binding.impl.ce;

import org.openl.binding.IBindingContext;
import org.openl.binding.IBoundNode;
import org.openl.binding.exception.FieldNotFoundException;
import org.openl.binding.impl.FieldBoundNode;
import org.openl.rules.calc.SpreadsheetResult;
import org.openl.rules.calc.SpreadsheetStructureBuilder;
import org.openl.syntax.ISyntaxNode;
import org.openl.types.IOpenClass;

public class MethodNodeBinder extends org.openl.binding.impl.MethodNodeBinder {
    @Override
    protected IBoundNode makeArrayParametersMethod(ISyntaxNode methodNode,
            IBindingContext bindingContext,
            String methodName,
            IOpenClass[] argumentTypes,
            IBoundNode[] children) throws Exception {
        return new ArrayArgumentsMethodBinder(methodName, argumentTypes, children).bind(methodNode, bindingContext);
    }

    @Override
    protected FieldBoundNode bindAsFieldBoundNode(ISyntaxNode methodNode,
            String methodName,
            IOpenClass[] argumentTypes,
            IBoundNode[] children,
            int childrenCount,
            IOpenClass argumentType,
            int dims) throws Exception {
        FieldBoundNode fieldBoundNode = super.bindAsFieldBoundNode(methodNode,
            methodName,
            argumentTypes,
            children,
            childrenCount,
            argumentType,
            dims);
        if (fieldBoundNode == null && methodName
            .startsWith(SpreadsheetStructureBuilder.DOLLAR_SIGN) && SpreadsheetResult.class
                .equals(argumentType.getInstanceClass())) {
            throw new FieldNotFoundException("", methodName, argumentType);
        }
        return fieldBoundNode;
    }
}
