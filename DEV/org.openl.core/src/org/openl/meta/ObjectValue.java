package org.openl.meta;

import java.util.Arrays;
import java.util.Date;

import org.openl.source.IOpenSourceCodeModule;
import org.openl.util.ArrayTool;

public class ObjectValue implements IMetaHolder, Comparable<ObjectValue> {

    private IMetaInfo metaInfo;
    private Object value;

    public ObjectValue(Object value) {
        if (value == null) {
            throw new IllegalArgumentException(
                "Error initializing ObjectValue class. Parameter \"value\" can't be null.");
        }
        this.value = value;
        metaInfo = new ValueMetaInfo();
    }

    public ObjectValue(Object value, String shortName, String fullName, IOpenSourceCodeModule source) {
        if (value == null) {
            throw new IllegalArgumentException(
                "Error initializing ObjectValue class. Parameter \"value\" can't be null.");
        }
        this.value = value;
        metaInfo = new ValueMetaInfo(shortName, fullName, source);
    }

    /**
     * Returns the Metainfo of the Object
     */
    @Override
    public IMetaInfo getMetaInfo() {
        return metaInfo;
    }

    /**
     * Sets the Metainfo of the Object
     */
    @Override
    public void setMetaInfo(IMetaInfo info) {
        this.metaInfo = info;
    }

    /**
     *
     * @return the value of current Objct
     */
    public Object getValue() {
        return value;
    }

    /**
     * Indicates whether some other object is "equal to" obj variable.
     */
    @Override
    public boolean equals(Object obj) {

        if (obj instanceof ObjectValue) {
            ObjectValue v = (ObjectValue) obj;
            return value.equals(v.value);
        }
        return obj != null && value.equals(obj);
    }

    /**
     * Returns the hashCode of the current value
     */
    @Override
    public int hashCode() {
        return value.hashCode();
    }

    /**
     * Compares the current object with the <b>objectToCompare</b>
     */
    @Override
    public int compareTo(ObjectValue objectToCompare) {
        int result = -1;
        if (value instanceof String && objectToCompare.getValue() instanceof String) {
            result = ((String) value).compareTo(((String) objectToCompare.getValue()));
        } else {
            if (value instanceof Date && objectToCompare.getValue() instanceof Date) {
                result = ((Date) value).compareTo(((Date) objectToCompare.getValue()));
            } else {
                if (value instanceof Boolean && objectToCompare.getValue() instanceof Boolean) {
                    result = ((Boolean) value).compareTo(((Boolean) objectToCompare.getValue()));
                } else {
                    if (value instanceof Integer && objectToCompare.getValue() instanceof Integer) {
                        result = ((Integer) value).compareTo(((Integer) objectToCompare.getValue()));
                    }
                }
            }
        }
        return result;
    }

    /**
     * Sorts the array <b>values</b>
     *
     * @param values array of Objects
     * @return the sorted array
     */
    public static ObjectValue[] sort(ObjectValue[] values) {
        ObjectValue[] sortedArray = null;
        if (values != null) {
            sortedArray = new ObjectValue[values.length];
            ObjectValue[] notNullArray = ArrayTool.removeNulls(values);
            Arrays.sort(notNullArray);

            /* Filling sortedArray by sorted and null values */
            System.arraycopy(notNullArray, 0, sortedArray, 0, notNullArray.length);

        }
        return sortedArray;
    }

}
