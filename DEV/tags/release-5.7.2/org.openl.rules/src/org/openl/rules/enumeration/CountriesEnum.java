package org.openl.rules.enumeration;

public enum CountriesEnum {

	AR("Argentina"),
	AU("Australia"),
	BY("Belarus"),
	BR("Brazil"),
	CA("Canada"),
	CN("China"),
	DE("Germany"),
	FR("France"),
	GB("United Kingdom"),
	HU("Hungary"),
	IN("India"),
	IT("Italy"),
	IL("Israel"),
	JP("Japan"),
	KR("Korea"),
	LV("Latvia"),
	LT("Lithuania"),
	MX("Mexico"),
	NZ("New Zealand"),
	PT("Portugal"),
	RU("Russian Federation"),
	CH("Switzerland"),
	TW("Taiwan"),
	TR("Turkey"),
	UA("Ukraine"),
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