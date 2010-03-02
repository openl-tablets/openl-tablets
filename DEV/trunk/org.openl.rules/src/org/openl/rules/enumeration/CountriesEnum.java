package org.openl.rules.enumeration;

public enum CountriesEnum {

	AR("Argentina"),
	AU("Australia"),
	BR("Brazil"),
	CA("Canada"),
	CH("China"),
	DE("Germany"),
	FR("France"),
	GB("United Kingdom"),
	IN("India"),
	IT("Italy"),
	MX("Mexico"),
	NZ("New Zealand"),
	PT("Portugal"),
	RU("Russian Federation"),
	US("United States of America"),
	ZA("South Africa");

	private final String displayName;

	private CountriesEnum (String displayName) {
		this.displayName = displayName;
	}

	@Override
	public String toString() {
		return displayName;
	}
}