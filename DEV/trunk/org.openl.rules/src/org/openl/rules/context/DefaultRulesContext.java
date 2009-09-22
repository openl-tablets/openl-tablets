package org.openl.rules.context;

import java.util.HashMap;
import java.util.Map;

public class DefaultRulesContext implements IRulesContext {
	
	private Map<String, Object> internalMap = new HashMap<String, Object>();
	
	public void setValue(String name, Object value) {
		internalMap.put(name, value);
	}
	
	public Object getValue(String name) {
		return internalMap.get(name);
	}
	
	// <<< INSERT >>>
	public java.util.Date getCurrentDate() {
		return (java.util.Date) internalMap.get("currentDate");
	}
	
	public void setCurrentDate(java.util.Date currentDate) {
		internalMap.put("currentDate", currentDate);
	}
	// <<< END INSERT >>>
}
