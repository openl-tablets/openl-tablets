package org.openl.rules.project.instantiation.variation;

import java.util.Stack;

import org.openl.exception.OpenlNotCheckedException;
import org.openl.types.IOpenField;
import org.openl.types.IOpenIndex;
import org.openl.types.java.JavaOpenClass;
import org.openl.types.java.OpenClassHelper;
import org.openl.vm.IRuntimeEnv;
import org.openl.vm.SimpleVM;

/**
 * Simple implementation of variation that can modify field values or set
 * elements for arrays. Uses java reflection.
 * 
 * Path to field can be specified as arrays of strings(where each element
 * specifies field of corresponding object or sequential number in array) or as
 * path in single string with delimeter(see
 * {@link FieldModificationVariation.PATH_DELIMETER})
 * 
 * Note: path can not point on root object(path "."), only fields modifications
 * supported. If you want to replace entire argument you should use
 * {@link ArgumentReplacementVariation}.
 * 
 * @author PUdalau
 * 
 */
// TODO investigate possibility to use method signature and precompile variation
// and avoid reflection
public class FieldModificationVariation extends Variation {
    private static final String PATH_DELIMETER_FOR_DISPLAYING = "->";
    private static final String THIS_POINTER = ".";
    public static final String PATH_DELIMETER = "/";
    private int updatedArgumentIndex;
    private String[] pathToField;
    private Object valueToSet;

    /**
     * Constructs variation.
     * 
     * @param variationID Unique ID of variation.
     * @param updatedArgumentIndex Index of argument to modify.
     * @param pathToField Path to field that will be modified.
     * @param valueToSet Value that will be set to field.
     */
    public FieldModificationVariation(String variationID,
            int updatedArgumentIndex,
            String[] pathToField,
            Object valueToSet) {
        super(variationID);
        if (updatedArgumentIndex < 0) {
            throw new IllegalArgumentException("Number of argument to be modified should be non negative.");
        } else {
            this.updatedArgumentIndex = updatedArgumentIndex;
        }
        this.pathToField = pathToField;
        this.valueToSet = valueToSet;
    }

    /**
     * Constructs variation.
     * 
     * @param variationID Unique ID of variation.
     * @param updatedArgumentIndex Index of argument to modify.
     * @param pathToField Path to field that will be modified with a delimeter
     *            {@link FieldModificationVariation.PATH_DELIMETER}
     * @param valueToSet Value that will be set to field.
     */
    public FieldModificationVariation(String variationID,
            int updatedArgumentIndex,
            String pathToField,
            Object valueToSet) {
        this(variationID, updatedArgumentIndex, pathToField.split(PATH_DELIMETER), valueToSet);
    }

    @Override
    public Object[] applyModification(Object[] originalArguments, Stack<Object> stack) {
        if (updatedArgumentIndex >= originalArguments.length) {
            throw new OpenlNotCheckedException("Failed to apply variaion \"" + getVariationID() + "\". Number of argument to modify is [" + updatedArgumentIndex + "] but arguments length is " + originalArguments.length);
        }
        Object updatableObject = originalArguments[updatedArgumentIndex];
        IRuntimeEnv env = new SimpleVM().getRuntimeEnv();
        updatableObject = findParentOfTheFieldToModify(updatableObject, env);
        Object previousValue;
        try {
            previousValue = getFieldValue(updatableObject, env, pathToField[pathToField.length - 1]);
        } catch (Exception e) {
            throw new OpenlNotCheckedException("Failed to get previous field value before the modification of argument with index [" + updatedArgumentIndex + "] by path :" + buildPath(pathToField,
                pathToField.length - 1), e);
        }
        stack.push(previousValue);
        try {
            setFieldValue(updatableObject, env, pathToField[pathToField.length - 1], valueToSet);
        } catch (Exception e) {
            throw new OpenlNotCheckedException("Failed to set field of argument with index [" + updatedArgumentIndex + "] by path :" + buildPath(pathToField,
                pathToField.length - 1), e);
        }
        return originalArguments;
    }

    @Override
    public void revertModifications(Object[] modifiedArguments, Stack<Object> stack) {
        Object updatableObject = modifiedArguments[updatedArgumentIndex];
        IRuntimeEnv env = new SimpleVM().getRuntimeEnv();
        updatableObject = findParentOfTheFieldToModify(updatableObject, env);
        Object previousValue = stack.pop();
        try {
            setFieldValue(updatableObject, env, pathToField[pathToField.length - 1], previousValue);
        } catch (Exception e) {
            throw new OpenlNotCheckedException("Failed to set field of argument with index [" + updatedArgumentIndex + "] by path :" + buildPath(pathToField,
                pathToField.length - 1), e);
        }
    }

    /**
     * Looks for parent object in bean tree that contains field to modify.
     * 
     * @param updatableObject root argument to be modified.
     * @param env {@link IRuntimeEnv} to access fields.
     * @return container for field to modify.
     */
    private Object findParentOfTheFieldToModify(Object updatableObject, IRuntimeEnv env) {
        for (int i = 0; i < pathToField.length - 1; i++) {
            if (pathToField[i] != THIS_POINTER) {
                try {
                    updatableObject = getFieldValue(updatableObject, env, pathToField[i]);
                } catch (Exception e) {
                    throw new OpenlNotCheckedException("Failed to get field of argument with index [" + updatedArgumentIndex + "] by path :" + buildPath(pathToField,
                        i), e);
                }
            }
        }
        return updatableObject;
    }

    /**
     * Get field value or array element using OpenL API. 
     */
    protected Object getFieldValue(Object updatableObject, IRuntimeEnv env, String fieldName) {
        JavaOpenClass openClass = JavaOpenClass.getOpenClass(updatableObject.getClass());
        if (OpenClassHelper.isCollection(openClass)) {
            IOpenIndex index = openClass.getAggregateInfo().getIndex(openClass, JavaOpenClass.INT);
            return index.getValue(updatableObject, Integer.parseInt(fieldName));
        } else {
            IOpenField field = openClass.getField(fieldName);
            return field.get(updatableObject, env);
        }
    }

    /**
     * Set field value or array element using OpenL API. 
     */
    protected void setFieldValue(Object updatableObject, IRuntimeEnv env, String fieldName, Object value) {
        JavaOpenClass openClass = JavaOpenClass.getOpenClass(updatableObject.getClass());
        if (OpenClassHelper.isCollection(openClass)) {
            IOpenIndex index = openClass.getAggregateInfo().getIndex(openClass, JavaOpenClass.INT);
            index.setValue(updatableObject, Integer.parseInt(fieldName), value);
        } else {
            IOpenField field = openClass.getField(fieldName);
            field.set(updatableObject, value, env);
        }
    }

    private static String buildPath(String[] path, int pathEntriesUsed) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < pathEntriesUsed; i++) {
            builder.append(path[i]);
            if (i != pathEntriesUsed - 1) {
                builder.append(PATH_DELIMETER_FOR_DISPLAYING);
            }
        }
        return builder.toString();
    }
    /**
     * @return Index of arguments to be modified.
     */
    public int getUpdatedArgumentIndex() {
        return updatedArgumentIndex;
    }

    /**
     *  @return path to field to be modified.
     */
    public String[] getPath() {
        return pathToField;
    }

    /**
     * @return value to set into modified field.
     */
    public Object getValueToSet() {
        return valueToSet;
    }
}
