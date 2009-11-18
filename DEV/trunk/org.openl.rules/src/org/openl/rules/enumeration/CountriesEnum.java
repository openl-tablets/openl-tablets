package org.openl.rules.enumeration;

public enum CountriesEnum {

	US("United States of America"),
	RU("Russian Federation"),
	BY("Belarus");

	private final String displayName;

	private CountriesEnum (Sting displayName) {
		this.displayName = displayName;
	}

	public String getDisplayName() {
		return displayName;
	}
}