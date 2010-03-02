package org.openl.rules.enumeration;

public enum CurrenciesEnum {

	ARS("Argentine Peso"),
	AUD("Australian Dollar"),
	BRL("Brazilian Real"),
	BYR("Belarusian Ruble"),
	CAD("Canadian Dollar"),
	CNY("Chinese Yuan"),
	EUR("Euro"),
	GBP("Pound Sterling"),
	INR("Indian rupee"),
	MXN("Mexican Peso"),
	NZD("New Zealand Dollar"),
	RUB("Russian Ruble"),
	USD("United States Dollar"),
	ZAR("South African Rand");

	private final String displayName;

	private CurrenciesEnum (String displayName) {
		this.displayName = displayName;
	}

	@Override
	public String toString() {
		return displayName;
	}
}