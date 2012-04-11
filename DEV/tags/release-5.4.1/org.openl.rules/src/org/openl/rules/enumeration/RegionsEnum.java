package org.openl.rules.enumeration;

public enum RegionsEnum {

	NCSA("Americas"),
	EMEA("Europe; Middle East; Africa"),
	APJ("Asia Pacific; Japan");

	private final String displayName;

	private RegionsEnum (String displayName) {
		this.displayName = displayName;
	}

	@Override
	public String toString() {
		return displayName;
	}
}