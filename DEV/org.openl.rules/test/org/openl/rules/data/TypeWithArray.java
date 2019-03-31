package org.openl.rules.data;

public class TypeWithArray {

    private String stringValue;
    private double doubleValue;
    private int[] intArray;
    private double[] doubleArray;
    private String[] stringArray;

    public ClassForStringConstructorLoadingTests getClassValues() {
        return classValues;
    }

    public void setClassValues(ClassForStringConstructorLoadingTests classValue) {
        this.classValues = classValue;
    }

    private ClassForStringConstructorLoadingTests classValues;

    public String getStringValue() {
        return stringValue;
    }

    public void setStringValue(String stringValue) {
        this.stringValue = stringValue;
    }

    public double getDoubleValue() {
        return doubleValue;
    }

    public void setDoubleValue(double doubleValue) {
        this.doubleValue = doubleValue;
    }

    public int[] getIntArray() {
        return intArray;
    }

    public void setIntArray(int[] intArray) {
        this.intArray = intArray;
    }

    public void setDoubleArray(double[] doubleArray) {
        this.doubleArray = doubleArray;
    }

    public double[] getDoubleArray() {
        return doubleArray;
    }

    public void setStringArray(String[] stringArray) {
        this.stringArray = stringArray;
    }

    public String[] getStringArray() {
        return stringArray;
    }

}
