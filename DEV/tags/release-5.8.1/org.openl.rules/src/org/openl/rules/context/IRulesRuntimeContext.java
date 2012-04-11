package org.openl.rules.context;

import org.openl.runtime.IRuntimeContext;

/*
 * Important notice:
 * If you add any methods, verify org.openl.rules.validation.properties.dimentional.DispatcherTableBuilder class works properly.
 * Refer to static initialization section were context attributes are gathered.
 * Add your method to exclusions the same way as it's done for "Object getValue(String variable)"
 */

public interface IRulesRuntimeContext extends IRuntimeContext {
	
	Object getValue(String variable);
	
	// <<< INSERT >>>
	java.util.Date getCurrentDate();
	void setCurrentDate(java.util.Date currentDate);	
	java.util.Date getRequestDate();
	void setRequestDate(java.util.Date requestDate);	
	java.lang.String getLob();
	void setLob(java.lang.String lob);	
	org.openl.rules.enumeration.UsStatesEnum getUsState();
	void setUsState(org.openl.rules.enumeration.UsStatesEnum usState);	
	org.openl.rules.enumeration.CountriesEnum getCountry();
	void setCountry(org.openl.rules.enumeration.CountriesEnum country);	
	org.openl.rules.enumeration.UsRegionsEnum getUsRegion();
	void setUsRegion(org.openl.rules.enumeration.UsRegionsEnum usRegion);	
	org.openl.rules.enumeration.CurrenciesEnum getCurrency();
	void setCurrency(org.openl.rules.enumeration.CurrenciesEnum currency);	
	org.openl.rules.enumeration.LanguagesEnum getLang();
	void setLang(org.openl.rules.enumeration.LanguagesEnum lang);	
	org.openl.rules.enumeration.RegionsEnum getRegion();
	void setRegion(org.openl.rules.enumeration.RegionsEnum region);	
	// <<< END INSERT >>>
}
