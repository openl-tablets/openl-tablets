package org.openl.rules.dt;

public class DTInfo {

    private final int numberHConditions;
    private final int numberVConditions;
    private DTScale scale = DTScale.STANDARD;
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

    public DTScale getScale() {
        return scale;
    }

    public int getNumberHConditions() {
        return numberHConditions;
    }

    public int getNumberVConditions() {
        return numberVConditions;
    }

    public boolean isTransposed() {
        return transposed;
    }
}
