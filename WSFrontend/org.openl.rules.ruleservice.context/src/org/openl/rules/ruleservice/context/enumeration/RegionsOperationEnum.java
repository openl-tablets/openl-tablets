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
public enum RegionsOperationEnum {

	QC("Québec"),
	HQ("Hors Québec");

	private final String displayName;

	private RegionsOperationEnum (String displayName) {
		this.displayName = displayName;
	}

	@Override
	public String toString() {
		return displayName;
	}
	
	public static RegionsOperationEnum fromString(String displayName) {
		for (RegionsOperationEnum v : RegionsOperationEnum.values()) {
			if (displayName.equalsIgnoreCase(v.displayName)) {
				return v;
			}
		}
		
		throw new IllegalArgumentException("No constant with displayName " + displayName + " found");
  	}
}