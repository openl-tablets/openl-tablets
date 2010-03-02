package org.openl.rules.enumeration;

public enum CurrenciesEnum {

	USD("United States Dollar"),
	AUD("Australian Dollar"),
	MXN("Mexican Peso"),
	NZD("New Zealand Dollar"),
	BRL("Brazilian Real"),
	ARS("Argentine Peso"),
	EUR("Euro"),
	CNY("Chinese Yuan"),
	INR("Indian rupee"),
	GBP("Pound Sterling"),
	ZAR("South African Rand"),
	RUB("Russian Ruble"),
	CAD("Canadian Dollar"),
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