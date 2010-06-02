/**
 * 
 */
package org.openl.rules.tableeditor.model;

public class RangeParam {
    private Long min, max;

    public RangeParam(Long min, Long max) {
        this.min = min;
        this.max = max;
    }

    public Long getMax() {
        return max;
    }

    public Long getMin() {
        return min;
    }

    public void setMax(Long max) {
        this.max = max;
    }

    public void setMin(Long min) {
        this.min = min;
    }
}