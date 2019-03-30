package org.openl.rules.dt;

public class DTInfo {

    private int numberHConditions = 0;
    private int numberVConditions;
    private DTScale scale = DTScale.STANDARD;

    DTInfo(int numberHConditions, int numberVConditions, DTScale scale) {
        super();
        this.numberHConditions = numberHConditions;
        this.numberVConditions = numberVConditions;
        this.scale = scale;
    }

    DTInfo(int numberHConditions, int numberVConditions) {
        super();
        this.numberHConditions = numberHConditions;
        this.numberVConditions = numberVConditions;
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

}
