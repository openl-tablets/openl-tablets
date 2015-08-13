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
public enum UsRegionsEnum {

	MW("Midwest"),
	NE("Northeast"),
	SE("Southeast"),
	SW("Southwest"),
	W("West");

	private final String displayName;

	private UsRegionsEnum (String displayName) {
		this.displayName = displayName;
	}

	@Override
	public String toString() {
		return displayName;
	}
	
	public static UsRegionsEnum fromString(String displayName) {
		for (UsRegionsEnum v : UsRegionsEnum.values()) {
			if (displayName.equalsIgnoreCase(v.displayName)) {
				return v;
			}
		}
		
		throw new IllegalArgumentException("No constant with displayName " + displayName + " found");
  	}
}