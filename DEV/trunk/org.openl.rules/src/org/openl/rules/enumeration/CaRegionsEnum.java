package org.openl.rules.enumeration;

public enum CaRegionsEnum {

	QC("Québec"),
	HQ("Hors Québec");

	private final String displayName;

	private CaRegionsEnum (String displayName) {
		this.displayName = displayName;
	}

	@Override
	public String toString() {
		return displayName;
	}
}