package org.openl.rules.enumeration;

public enum LanguagesEnum {

	EN("English"),
	POR("Portuguese"),
	RUS("Russian"),
	FRA("French"),
	ITA("Italian"),
	CHI("Chinese"),
	GER("German");

	private final String displayName;

	private LanguagesEnum (String displayName) {
		this.displayName = displayName;
	}

	@Override
	public String toString() {
		return displayName;
	}
}