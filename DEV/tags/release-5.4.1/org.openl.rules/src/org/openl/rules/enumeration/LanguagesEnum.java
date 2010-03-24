package org.openl.rules.enumeration;

public enum LanguagesEnum {

	CHI("Chinese"),
	EN("English"),
	FRA("French"),
	GER("German"),
	ITA("Italian"),
	POR("Portuguese"),
	RUS("Russian");

	private final String displayName;

	private LanguagesEnum (String displayName) {
		this.displayName = displayName;
	}

	@Override
	public String toString() {
		return displayName;
	}
}