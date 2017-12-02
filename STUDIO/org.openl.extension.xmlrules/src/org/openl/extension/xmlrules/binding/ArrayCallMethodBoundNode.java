package org.openl.extension.xmlrules.binding;

import java.lang.reflect.Array;
import java.util.List;

import org.openl.binding.IBoundNode;
import org.openl.binding.impl.MethodBoundNode;
import org.openl.syntax.ISyntaxNode;
import org.openl.types.IMethodCaller;
import org.openl.types.IOpenClass;
import org.openl.types.java.JavaOpenClass;
import org.openl.vm.IRuntimeEnv;

public class ArrayCallMethodBoundNode extends MethodBoundNode {
    private final List<Integer> arrayArgArguments;

    /**
     * Cached return type for current bound node
     */
    private IOpenClass returnType;

    public ArrayCallMethodBoundNode(ISyntaxNode syntaxNode,
            IBoundNode[] child,
            IMethodCaller methodCaller, List<Integer> arrayArgArguments) {
        super(syntaxNode, child, methodCaller);
        this.arrayArgArguments = arrayArgArguments;
    }

    @Override
    protected Object evaluateRuntime(IRuntimeEnv env) {
        Object target = getTargetNode() == null ? env.getThis() : getTargetNode().evaluate(env);
        Object[] methodParameters = evaluateChildren(env);

        convertToTwoDimensionalArrays(methodParameters);

        int maxHeight = 0;
        int maxWidth = 0;

        for (Integer arrayArgArgument : arrayArgArguments) {
            Object methodParameter = methodParameters[arrayArgArgument];
            if (methodParameter != null) {
                maxHeight = Math.max(maxHeight, Array.getLength(methodParameter));
                maxWidth = Math.max(maxWidth, Array.getLength(Array.get(methodParameter, 0)));
            }
        }

        // Create an array of results
        Object results = Array.newInstance(getSingleReturnType().getInstanceClass(), maxHeight, maxWidth);

        // Populate the results array by invoking method for single parameter
        for (int row = 0; row < maxHeight; row++) {
            for (int column = 0; column < maxWidth; column++) {
                Object[] singleCallParams = getParametersForSingleCall(methodParameters, row, column);
                Object result = getMethodCaller().invoke(target, singleCallParams, env);

                Array.set(Array.get(results, row), column, result);
            }
        }

        if (JavaOpenClass.VOID.equals(getSingleReturnType())) {
            return null;
        }

        return results;
    }

    public IOpenClass getSingleReturnType() {
        if (getMethodCaller() instanceof PoiMethodCaller) {
            return JavaOpenClass.OBJECT;
        }
        return super.getType();
    }

    @Override
    public IOpenClass getType() {
        if (returnType == null) {
            returnType = getReturnType();
        }
        return returnType;
    }

    private IOpenClass getReturnType() {
        IOpenClass singleReturnType = getSingleReturnType();

        if (JavaOpenClass.VOID.equals(singleReturnType)) {
            return JavaOpenClass.VOID;
        } else {
            // Create an array type.
            return singleReturnType.getAggregateInfo().getIndexedAggregateType(singleReturnType, 2);
        }
    }

    private void convertToTwoDimensionalArrays(Object[] methodParameters) {
        for (Integer arrayArgArgument : arrayArgArguments) {
            Object methodParameter = methodParameters[arrayArgArgument];
            if (methodParameter != null && methodParameter.getClass().isArray()) {
                if (Array.getLength(methodParameter) == 0) {
                    methodParameters[arrayArgArgument] = new Object[][] { { } };
                    continue;
                }
                Object row = Array.get(methodParameter, 0);
                if (!row.getClass().isArray()) {
                    methodParameters[arrayArgArgument] = new Object[][] { (Object[]) methodParameter };
                }
            } else {
                methodParameters[arrayArgArgument] = new Object[][] { { methodParameter } };
            }
        }
    }

    private Object[] getParametersForSingleCall(Object[] allParameters,
            final int currentRowNum,
            final int currentColumnNum) {
        // Create an array of parameters that will be used for current call
        Object[] callParameters = (Object[]) Array.newInstance(Object.class, allParameters.length);

        // Populate call parameters with values from original method parameters
        for (int i = 0; i < allParameters.length; i++) {
            Object parameter = allParameters[i];
            if (arrayArgArguments.contains(i)) {
                // For currentRowNum and currentColumnNum use the appropriate value from the array call parameter
                int rowsCount = Array.getLength(parameter);
                int rowNum = currentRowNum;
                if (rowNum >= rowsCount) {
                    if (rowsCount > 1) {
                        continue;
                    } else {
                        rowNum = 0;
                    }
                }

                Object row = Array.get(parameter, rowNum);
                int colCount = Array.getLength(row);
                int columnNum = currentColumnNum;
                if (columnNum >= colCount) {
                    if (colCount > 1) {
                        continue;
                    } else {
                        columnNum = 0;
                    }
                }

                Array.set(callParameters, i, Array.get(row, columnNum));
            } else {
                // Use the original parameter.
                Array.set(callParameters, i, parameter);
            }
        }
        return callParameters;
    }
}
