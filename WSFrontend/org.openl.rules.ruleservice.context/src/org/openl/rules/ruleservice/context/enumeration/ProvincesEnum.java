package org.openl.rules.ruleservice.context.enumeration;

/*
 * #%L
 * OpenL - RuleService - RuleService - Context
 * %%
 * Copyright (C) 2013 OpenL Tablets
 * %%
 * See the file LICENSE.txt for copying permission.
 * #L%
 */
public enum ProvincesEnum {

	AB("Alberta"),
	BC("Colombie-Britannique"),
	PE("Île-du-Prince-Édouard"),
	MB("Manitoba"),
	NB("Nouveau-Brunswick"),
	NS("Nouvelle-Écosse"),
	NU("Nunavut"),
	ON("Ontario"),
	QC("Québec"),
	SK("Saskatchewan"),
	NL("Terre-Neuve-et-Labrador"),
	YT("Yukon"),
	NT("Territoires du Nord-Ouest");

	private final String displayName;

	private ProvincesEnum (String displayName) {
		this.displayName = displayName;
	}

	@Override
	public String toString() {
		return displayName;
	}
	
	public static ProvincesEnum fromString(String displayName) {
		for (ProvincesEnum v : ProvincesEnum.values()) {
			if (displayName.equalsIgnoreCase(v.displayName)) {
				return v;
			}
		}
		
		throw new IllegalArgumentException("No constant with displayName " + displayName + " found");
  	}
}