package org.openl.rules.dt;

public class DTInfo {
	
	int numberHConditions = 0;
	int numberVConditions;
	DTScale scale = DTScale.STANDARD;
	
	public DTInfo(int numberHConditions, int numberVConditions, DTScale scale) {
		super();
		this.numberHConditions = numberHConditions;
		this.numberVConditions = numberVConditions;
		this.scale = scale;
	}

	public DTInfo(int numberHConditions, int numberVConditions) {
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
