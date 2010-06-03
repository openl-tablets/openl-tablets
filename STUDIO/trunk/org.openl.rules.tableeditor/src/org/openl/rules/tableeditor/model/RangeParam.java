/**
 * 
 */
package org.openl.rules.tableeditor.model;

public class RangeParam {

    private Number min;
    private Number max;

    public RangeParam(Number min, Number max) {
        this.min = min;
        this.max = max;
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
}