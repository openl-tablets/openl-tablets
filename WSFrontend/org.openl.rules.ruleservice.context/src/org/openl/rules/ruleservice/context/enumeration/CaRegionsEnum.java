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
public enum CaRegionsEnum {

	QC("Québec"),
	HQ("Hors Québec");

	private final String displayName;

	private CaRegionsEnum (String displayName) {
		this.displayName = displayName;
	}

	@Override
	public String toString() {
		return displayName;
	}
	
	public static CaRegionsEnum fromString(String displayName) {
		for (CaRegionsEnum v : CaRegionsEnum.values()) {
			if (displayName.equalsIgnoreCase(v.displayName)) {
				return v;
			}
		}
		
		throw new IllegalArgumentException("No constant with displayName " + displayName + " found");
  	}
}