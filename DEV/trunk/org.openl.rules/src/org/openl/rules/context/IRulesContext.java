package org.openl.rules.context;

import org.openl.runtime.IContext;

public interface IRulesContext extends IContext {
	
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
	java.lang.String getCountry();
	void setCountry(java.lang.String country);	
	// <<< END INSERT >>>
}
