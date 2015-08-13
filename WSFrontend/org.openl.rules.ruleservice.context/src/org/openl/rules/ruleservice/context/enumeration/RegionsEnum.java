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
public enum RegionsEnum {

	NCSA("Americas"),
	EU("European Union"),
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
	
	public static RegionsEnum fromString(String displayName) {
		for (RegionsEnum v : RegionsEnum.values()) {
			if (displayName.equalsIgnoreCase(v.displayName)) {
				return v;
			}
		}
		
		throw new IllegalArgumentException("No constant with displayName " + displayName + " found");
  	}
}