package org.openl.binding.impl;

import java.lang.reflect.Array;

import org.openl.binding.BindingDependencies;
import org.openl.binding.IBoundNode;
import org.openl.exception.OpenLRuntimeException;
import org.openl.syntax.ISyntaxNode;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.vm.IRuntimeEnv;

/**
 * This node handles both a single value and a multi-dimension arrays.
 *
 * Examples:
 * <p>
 * Object value = target.filed
 * </p>
 * <p>
 * Object[][] value = target[][].filed
 * </p>
 * 
 * @author Yury Molchan
 * 
 */
public class FieldBoundNode extends ATargetBoundNode {

    private final IOpenField boundField;
    private final int dims;
    private IOpenClass returnType;

    FieldBoundNode(ISyntaxNode syntaxNode, IOpenField field) {
        this(syntaxNode, field, null);
    }

    FieldBoundNode(ISyntaxNode syntaxNode, IOpenField field, IBoundNode target) {
        super(syntaxNode, new IBoundNode[0], target);
        this.dims = 0;
        boundField = field;
    }

    FieldBoundNode(ISyntaxNode syntaxNode, IOpenField field, IBoundNode target, int dims) {
        super(syntaxNode, new IBoundNode[0], target);
        this.dims = dims;
        boundField = field;
    }

    @Override
    public void assign(Object value, IRuntimeEnv env) throws OpenLRuntimeException {
        Object target = getTargetNode() == null ? env.getThis() : getTargetNode().evaluate(env);

        boundField.set(target, value, env);
    }

    public String getFieldName() {
        return boundField.getName();
    }

    public IOpenField getBoundField() {
        return boundField;
    }

    @Override
    public Object evaluateRuntime(IRuntimeEnv env) throws OpenLRuntimeException {
        Object target = getTargetNode() == null ? env.getThis() : getTargetNode().evaluate(env);

        return evaluateDim(target, env, dims, null);
    }

    private Object evaluateDim(Object target, IRuntimeEnv env, int dims, Class<?> targetType) {
        if (dims == 0){
            return boundField.get(target, env);
        } else if (target == null) {
            return null;
        } else {
            int paramsLenght = Array.getLength(target);

            // create an array of results
            if (targetType == null) {
                targetType = getType().getInstanceClass();
            }
            Class<?> componentType = targetType.getComponentType();
            int nextDim = --dims;
            Object results = Array.newInstance(componentType, paramsLenght);

            // populate the results array by invoking method for single parameter
            for (int i = 0; i < paramsLenght; i++) {
                Object element = Array.get(target, i);
                Object value = evaluateDim(element, env, nextDim, componentType);
                // Do not try to set null value in primitive type.
                // And no needs to set null values for a just initialized array.
                if (value != null) {
                    Array.set(results, i, value);
                }
            }
            return results;
        }
    }

    @Override
    public IOpenClass getType() {
        if (returnType == null) {
            returnType = boundField.getType();
            if (dims > 0) {
                returnType = returnType.getAggregateInfo().getIndexedAggregateType(returnType, dims);
            }
        }
        return returnType;
    }

    @Override
    public boolean isLvalue() {
        return boundField.isWritable();
    }

    @Override
    public void updateAssignFieldDependency(BindingDependencies dependencies) {
        dependencies.addAssignField(boundField, this);
    }

    @Override
    public void updateDependency(BindingDependencies dependencies) {
        dependencies.addFieldDependency(boundField, this);
    }

    @Override
    public boolean isLiteralExpressionParent() {
        return boundField.isConst();
    }
}
