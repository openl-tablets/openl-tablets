package org.openl.rules.tableeditor.model;

public class RangeParam {

    private Number min;
    private Number max;
    private boolean intOnly;

    public RangeParam(Number min, Number max, boolean intOnly) {
        this.min = min;
        this.max = max;
        this.intOnly = intOnly;
    }

    public Number getMax() {
        return max;
    }

    public Number getMin() {
        return min;
    }

    public void setMax(Number max) {
        this.max = max;
    }

    public void setMin(Number min) {
        this.min = min;
    }

    public boolean isIntOnly() {
        return intOnly;
    }

    public void setIntOnly(boolean intOnly) {
        this.intOnly = intOnly;
    }
}