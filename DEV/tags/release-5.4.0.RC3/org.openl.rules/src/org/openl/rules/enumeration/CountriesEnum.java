package org.openl.rules.enumeration;

public enum CountriesEnum {

	US("United States of America"),
	RU("Russian Federation"),
	BY("Belarus");

	private final String displayName;

	private CountriesEnum (String displayName) {
		this.displayName = displayName;
	}

	@Override
	public String toString() {
		return displayName;
	}
}