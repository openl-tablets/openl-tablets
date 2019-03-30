package org.openl.rules.helpers;

public class DoubleHolder implements IDoubleHolder {
    double value;

    public DoubleHolder() {
    }

    public DoubleHolder(double value) {
        this.value = value;
    }

    @Override
    public double getValue() {
        return value;
    }

    @Override
    public void setValue(double value) {
        this.value = value;
    }

}