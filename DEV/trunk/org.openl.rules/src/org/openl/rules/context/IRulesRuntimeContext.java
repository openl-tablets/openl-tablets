package org.openl.rules.context;

import org.openl.runtime.IRuntimeContext;

public interface IRulesRuntimeContext extends IRuntimeContext {
    // Empty contains that can be used as default. All tables regardless values
    // of their properties will be matched for this context.
    public static IRulesRuntimeContext EMPTY_CONTEXT = new DefaultRulesRuntimeContext();
	
	Object getValue(String variable);
	
	// <<< INSERT >>>
	java.util.Date getCurrentDate();
	void setCurrentDate(java.util.Date currentDate);	
	java.lang.String getLob();
	void setLob(java.lang.String lob);	
	org.openl.rules.enumeration.UsregionsEnum getUsRegion();
	void setUsRegion(org.openl.rules.enumeration.UsregionsEnum usRegion);	
	org.openl.rules.enumeration.UsstatesEnum getUsState();
	void setUsState(org.openl.rules.enumeration.UsstatesEnum usState);	
	org.openl.rules.enumeration.CountriesEnum getCountry();
	void setCountry(org.openl.rules.enumeration.CountriesEnum country);	
	// <<< END INSERT >>>
}
