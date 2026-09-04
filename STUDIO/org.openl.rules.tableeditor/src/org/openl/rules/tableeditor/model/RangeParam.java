package org.openl.rules.tableeditor.model;

import lombok.Getter;
import lombok.Setter;

public class RangeParam {

    @Getter
    @Setter
    private Number min;
    @Getter
    @Setter
    private Number max;
    @Getter
    @Setter
    private boolean intOnly;

    public RangeParam(Number min, Number max, boolean intOnly) {
        this.min = min;
        this.max = max;
        this.intOnly = intOnly;
    }
}
