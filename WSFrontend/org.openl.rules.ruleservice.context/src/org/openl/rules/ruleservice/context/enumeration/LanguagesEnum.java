package org.openl.rules.ruleservice.context.enumeration;

/*
 * #%L
 * OpenL - RuleService - RuleService - Context
 * %%
 * Copyright (C) 2015 OpenL Tablets
 * %%
 * See the file LICENSE.txt for copying permission.
 * #L%
 */
public enum LanguagesEnum {

	ALB("Albanian"),
	ARA("Arabic"),
	BEL("Belarussian"),
	BUL("Bulgarian"),
	CAT("Catalan"),
	CHI("Chinese"),
	SCR("Croatian"),
	CZE("Czech"),
	DAN("Danish"),
	DUT("Dutch"),
	ENG("English"),
	FIN("Finnish"),
	FRE("French"),
	GER("German"),
	GRE("Greek"),
	HEB("Hebrew"),
	HIN("Hindi"),
	HUN("Hungarian"),
	IND("Indonesian"),
	GLE("Irish"),
	ITA("Italian"),
	JPN("Japanese"),
	LAV("Latvian"),
	LIT("Lithuanian"),
	MAC("Macedonian"),
	MAY("Malay"),
	MLT("Maltese"),
	NOR("Norwegian"),
	POL("Polish"),
	POR("Portuguese"),
	RUM("Romanian"),
	RUS("Russian"),
	SCC("Serbian"),
	SLO("Slovak"),
	SPA("Spanish"),
	THA("Thai"),
	TUR("Turkish"),
	UKR("Ukrainian"),
	VIE("Vietnamese");

	private final String displayName;

	private LanguagesEnum (String displayName) {
		this.displayName = displayName;
	}

	@Override
	public String toString() {
		return displayName;
	}
	
	public static LanguagesEnum fromString(String displayName) {
		for (LanguagesEnum v : LanguagesEnum.values()) {
			if (displayName.equalsIgnoreCase(v.displayName)) {
				return v;
			}
		}
		
		throw new IllegalArgumentException("No constant with displayName " + displayName + " found");
  	}
}