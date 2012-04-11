package org.openl.rules.enumeration;

public enum CurrenciesEnum {

	USD("United States Dollar"),
	RUB("Russian Ruble"),
	BYR("Belarusian Ruble");

	private final String displayName;

	private CurrenciesEnum (String displayName) {
		this.displayName = displayName;
	}

	@Override
	public String toString() {
		return displayName;
	}
}