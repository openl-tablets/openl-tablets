package org.openl.rules.enumeration;

public enum CountriesEnum {

	US("United States of America"),
	AU("Australia"),
	MX("Mexico"),
	NZ("New Zealand"),
	BR("Brazil"),
	AR("Argentina"),
	FR("France"),
	DE("Germany"),
	CH("China"),
	IT("Italy"),
	PT("Portugal"),
	IN("India"),
	GB("United Kingdom"),
	ZA("South Africa"),
	RU("Russian Federation"),
	CA("Canada");

	private final String displayName;

	private CountriesEnum (String displayName) {
		this.displayName = displayName;
	}

	@Override
	public String toString() {
		return displayName;
	}
}