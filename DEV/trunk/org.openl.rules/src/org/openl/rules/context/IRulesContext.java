package org.openl.rules.context;

import org.openl.runtime.IContext;

public interface IRulesContext extends IContext {
	
	Object getValue(String variable);
	
	// <<< INSERT >>>
	java.util.Date getCurrentDate();
	void setCurrentDate(java.util.Date currentDate);	
	// <<< END INSERT >>>
}
