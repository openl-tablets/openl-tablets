package org.openl.rules.context;

import org.openl.runtime.IRuntimeContext;

public interface IRulesRuntimeContext extends IRuntimeContext {
	
	Object getValue(String variable);
	
	// <<< INSERT >>>
	java.util.Date getCurrentDate();
	void setCurrentDate(java.util.Date currentDate);	
	java.lang.String getLob();
	void setLob(java.lang.String lob);	
	java.lang.String getUsRegion();
	void setUsRegion(java.lang.String usRegion);	
	java.lang.String getUsState();
	void setUsState(java.lang.String usState);	
	org.openl.rules.enumeration.CountriesEnum getCountry();
	void setCountry(org.openl.rules.enumeration.CountriesEnum country);	
	// <<< END INSERT >>>
}
