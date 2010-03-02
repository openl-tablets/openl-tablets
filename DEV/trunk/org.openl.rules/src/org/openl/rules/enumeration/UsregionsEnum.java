package org.openl.rules.enumeration;

public enum UsregionsEnum {

	W("West"),
	SW("Southwest"),
	MW("Midwest"),
	SE("Southeast"),
	NE("Northeast");

	private final String displayName;

	private UsregionsEnum (String displayName) {
		this.displayName = displayName;
	}

	@Override
	public String toString() {
		return displayName;
	}
}