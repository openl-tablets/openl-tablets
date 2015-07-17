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
public enum RecalculateEnum {

	ALWAYS("Always"),
	NEVER("Never"),
	ANALYZE("Analyze");

	private final String displayName;

	private RecalculateEnum (String displayName) {
		this.displayName = displayName;
	}

	@Override
	public String toString() {
		return displayName;
	}
	
	public static RecalculateEnum fromString(String displayName) {
		for (RecalculateEnum v : RecalculateEnum.values()) {
			if (displayName.equalsIgnoreCase(v.displayName)) {
				return v;
			}
		}
		
		throw new IllegalArgumentException("No constant with displayName " + displayName + " found");
  	}
}