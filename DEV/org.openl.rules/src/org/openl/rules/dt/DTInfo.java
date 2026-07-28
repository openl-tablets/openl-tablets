package org.openl.rules.dt;

import lombok.Getter;

public class DTInfo {

    @Getter
    private final int numberHConditions;
    @Getter
    private final int numberVConditions;
    @Getter
    private DTScale scale = DTScale.STANDARD;
    @Getter
    private boolean transposed;

    DTInfo(int numberHConditions, int numberVConditions, DTScale scale, boolean transposed) {
        super();
        this.numberHConditions = numberHConditions;
        this.numberVConditions = numberVConditions;
        this.scale = scale;
        this.transposed = transposed;
    }

    DTInfo(int numberHConditions, int numberVConditions, boolean transposed) {
        super();
        this.numberHConditions = numberHConditions;
        this.numberVConditions = numberVConditions;
        this.transposed = transposed;
    }
}
