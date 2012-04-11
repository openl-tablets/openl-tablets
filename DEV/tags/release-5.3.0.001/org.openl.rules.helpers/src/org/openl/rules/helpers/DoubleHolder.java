package org.openl.rules.helpers;

public class DoubleHolder implements IDoubleHolder {
    double value;

    public DoubleHolder() {
    }

    public DoubleHolder(double value) {
        this.value = value;
    }

    public double getValue() {
        return value;
    }

    public void setValue(double value) {
        this.value = value;
    }

}